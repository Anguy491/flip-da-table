package com.flip.backend.lasvegas.engine.phase;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.game.engine.phase.RuntimePhase;
import com.flip.backend.lasvegas.bot.LasVegasBotStrategy;
import com.flip.backend.lasvegas.bot.LasVegasBotTicket;
import com.flip.backend.lasvegas.engine.LasVegasSnapshot;
import com.flip.backend.lasvegas.engine.event.EndGameEvent;
import com.flip.backend.lasvegas.engine.event.PlaceDiceEvent;
import com.flip.backend.lasvegas.engine.event.RollDiceEvent;
import com.flip.backend.lasvegas.engine.event.SkipTurnEvent;
import com.flip.backend.lasvegas.engine.view.LasVegasView.ActionLogEntry;
import com.flip.backend.lasvegas.engine.view.LasVegasView.CasinoView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PlacementView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PlayerView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import com.flip.backend.lasvegas.engine.view.LasVegasView.ResultView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.RollView;
import com.flip.backend.lasvegas.entities.LasVegasBoard;
import com.flip.backend.lasvegas.entities.LasVegasCasino;
import com.flip.backend.lasvegas.entities.LasVegasDieRoll;
import com.flip.backend.lasvegas.entities.LasVegasMoneyCard;
import com.flip.backend.lasvegas.entities.LasVegasMoneyDeck;
import com.flip.backend.lasvegas.entities.LasVegasPlayer;
import com.flip.backend.security.GameStateConflictException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Event-driven aggregate for all three casino rounds in one platform game. */
public final class LasVegasRuntimePhase extends RuntimePhase {
    public static final int SCHEMA_VERSION = 2;
    public static final int TOTAL_ROUNDS = 3;

    public enum State {
        WAITING_FOR_ROLL,
        WAITING_FOR_CHOICE,
        RESOLVING,
        FINISHED
    }

    public record Command(long expectedVersion, String type, Integer face) {}
    public record CommandBatch(List<PublicEvent> publicEvents) {}

    private final Random random;
    private final LasVegasBoard board;
    private final LasVegasMoneyDeck deck;
    private final List<LasVegasCasino> casinos;
    private final List<ActionLogEntry> actionLog = new ArrayList<>();
    private final List<PublicEvent> transientEvents = new ArrayList<>();
    private final Map<Integer, String> jackpotWinners = new HashMap<>();

    private State state = State.WAITING_FOR_ROLL;
    private int internalRound = 1;
    private long stateVersion;
    private long eventSequence;
    private String roundStarterId;
    private List<LasVegasDieRoll> currentRoll = List.of();
    private List<ResultView> results = List.of();
    private LasVegasEndingPhase endingPhase;

    private LasVegasRuntimePhase(
            LasVegasBoard board,
            LasVegasMoneyDeck deck,
            List<LasVegasCasino> casinos,
            Random random
    ) {
        this.board = board;
        this.deck = deck;
        this.casinos = casinos;
        this.random = Objects.requireNonNull(random, "random");
    }

    public static LasVegasRuntimePhase newGame(List<PlayerStartInfo> playerInfos, Random random) {
        Objects.requireNonNull(playerInfos, "playerInfos");
        Objects.requireNonNull(random, "random");
        if (playerInfos.size() < 3 || playerInfos.size() > 10) {
            throw new IllegalArgumentException("Las Vegas requires 3-10 players");
        }
        var players = playerInfos.stream()
                .map(info -> new LasVegasPlayer(info.playerId(), info.name(), info.bot()))
                .toList();
        var board = new LasVegasBoard(players);
        board.moveToPlayer(players.get(random.nextInt(players.size())).getId());
        var deck = new LasVegasMoneyDeck(random);
        deck.initialize();
        var casinos = new ArrayList<LasVegasCasino>(6);
        for (int number = 1; number <= 6; number++) casinos.add(new LasVegasCasino(number));

        var runtime = new LasVegasRuntimePhase(board, deck, List.copyOf(casinos), random);
        runtime.roundStarterId = board.currentPlayer().getId();
        players.forEach(LasVegasPlayer::addRoundChips);
        runtime.dealCasinoBonuses();
        runtime.appendEvent("ROUND_STARTED", runtime.roundStarterId, null, null, null, null, null,
                "Casino round 1 started", true);
        return runtime;
    }

    public static LasVegasRuntimePhase restore(LasVegasSnapshot snapshot) {
        return restore(snapshot, new SecureRandom());
    }

    public static LasVegasRuntimePhase restore(LasVegasSnapshot snapshot, Random random) {
        if (snapshot == null || snapshot.schemaVersion() < 1 || snapshot.schemaVersion() > SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported Las Vegas snapshot schema");
        }
        var players = new ArrayList<LasVegasPlayer>();
        for (var playerState : snapshot.players()) {
            var player = new LasVegasPlayer(playerState.playerId(), playerState.name(), playerState.bot());
            player.restore(
                    playerState.chips(),
                    playerState.remainingRegularDice(),
                    playerState.bigDieRemaining(),
                    playerState.moneyCards()
            );
            players.add(player);
        }
        var board = new LasVegasBoard(players);
        board.restoreCurrentPlayer(snapshot.currentPlayerId(), snapshot.turnCount());
        var deck = new LasVegasMoneyDeck(random);
        deck.restoreAmounts(snapshot.deck());
        var casinos = new ArrayList<LasVegasCasino>();
        var jackpotWinners = new HashMap<Integer, String>();
        for (var casinoState : snapshot.casinos()) {
            var casino = new LasVegasCasino(casinoState.number());
            var placements = new LinkedHashMap<String, LasVegasCasino.Placement>();
            for (var placement : casinoState.placements()) {
                placements.put(placement.playerId(), new LasVegasCasino.Placement(placement.regularDice(), placement.bigDie()));
            }
            casino.restore(casinoState.jackpot(), casinoState.secondPrize(), placements);
            casinos.add(casino);
            if (casinoState.jackpotWinnerId() != null) jackpotWinners.put(casino.number(), casinoState.jackpotWinnerId());
        }
        var runtime = new LasVegasRuntimePhase(board, deck, List.copyOf(casinos), random);
        runtime.internalRound = snapshot.internalRound();
        runtime.state = State.valueOf(snapshot.phase());
        runtime.stateVersion = snapshot.stateVersion();
        runtime.eventSequence = snapshot.eventSequence();
        runtime.roundStarterId = snapshot.roundStarterId();
        runtime.currentRoll = snapshot.currentRoll().stream()
                .map(roll -> new LasVegasDieRoll(roll.face(), roll.big()))
                .toList();
        runtime.actionLog.addAll(snapshot.actionLog());
        runtime.results = List.copyOf(snapshot.results());
        runtime.jackpotWinners.putAll(jackpotWinners);
        if (runtime.state == State.FINISHED) runtime.endingPhase = new LasVegasEndingPhase(runtime.results);
        return runtime;
    }

    @Override
    public void enter() {
        // The aggregate is fully initialized by the start phase or snapshot restore.
    }

    @Override
    public String run() {
        return state.name();
    }

    public synchronized CommandBatch applyCommand(String playerId, Command command) {
        Objects.requireNonNull(command, "command");
        if (command.expectedVersion() != stateVersion) {
            throw new GameStateConflictException("expected version does not match current state");
        }
        LasVegasPlayer player = player(playerId);
        EventQueue queue = new EventQueue();
        String type = command.type() == null ? "" : command.type().trim().toUpperCase();
        switch (type) {
            case "ROLL_DICE" -> queue.enqueue(new RollDiceEvent(this, player));
            case "PLACE_DICE" -> {
                if (command.face() == null) throw new IllegalArgumentException("face is required");
                queue.enqueue(new PlaceDiceEvent(this, player, command.face(), queue));
            }
            case "SKIP_TURN" -> queue.enqueue(new SkipTurnEvent(this, player));
            default -> throw new IllegalArgumentException("unsupported Las Vegas command");
        }
        while (!queue.isEmpty()) {
            var event = queue.poll();
            if (!event.isValid()) throw new IllegalArgumentException("command is not legal in the current state");
            event.execute();
        }
        stateVersion++;
        var emitted = List.copyOf(transientEvents);
        transientEvents.clear();
        return new CommandBatch(emitted);
    }

    public boolean canRoll(LasVegasPlayer player) {
        return isCurrent(player) && state == State.WAITING_FOR_ROLL && player.hasDiceRemaining();
    }

    public boolean canPlace(LasVegasPlayer player, int face) {
        return isCurrent(player)
                && state == State.WAITING_FOR_CHOICE
                && face >= 1 && face <= 6
                && currentRoll.stream().anyMatch(roll -> roll.face() == face);
    }

    public boolean canSkip(LasVegasPlayer player) {
        return isCurrent(player) && state == State.WAITING_FOR_CHOICE && player.chips() > 0;
    }

    public void rollDice(LasVegasPlayer player) {
        var rolled = new ArrayList<LasVegasDieRoll>(player.remainingDiceCount());
        for (int index = 0; index < player.remainingRegularDice(); index++) {
            rolled.add(new LasVegasDieRoll(random.nextInt(6) + 1, false));
        }
        if (player.bigDieRemaining()) rolled.add(new LasVegasDieRoll(random.nextInt(6) + 1, true));
        currentRoll = List.copyOf(rolled);
        state = State.WAITING_FOR_CHOICE;
        appendEvent("ROLL_DICE", player.getId(), null, null, rolled.size(), null, null,
                player.name() + " rolled " + rolled.size() + " dice", true);
    }

    public void placeDice(LasVegasPlayer player, int face, EventQueue queue) {
        int regular = (int) currentRoll.stream().filter(roll -> roll.face() == face && !roll.big()).count();
        boolean big = currentRoll.stream().anyMatch(roll -> roll.face() == face && roll.big());
        player.placeDice(regular, big);
        casino(face).place(player.getId(), regular, big);
        currentRoll = List.of();
        board.tickTurn();
        appendEvent("PLACE_DICE", player.getId(), face, face, regular, big, null,
                player.name() + " placed dice at casino " + face, true);

        if (board.seats().stream().noneMatch(LasVegasPlayer::hasDiceRemaining)) {
            state = State.RESOLVING;
            for (int number = 1; number <= 6; number++) {
                queue.enqueue(new com.flip.backend.lasvegas.engine.event.ResolveCasinoEvent(this, player, number));
            }
            if (internalRound >= TOTAL_ROUNDS) queue.enqueue(new EndGameEvent(this, player));
            else queue.enqueue(new com.flip.backend.lasvegas.engine.event.AdvanceRoundEvent(this, player));
            return;
        }
        board.advanceToNextEligible();
        state = State.WAITING_FOR_ROLL;
    }

    public void skipTurn(LasVegasPlayer player) {
        player.spendChip();
        currentRoll = List.of();
        board.tickTurn();
        appendEvent("SKIP_TURN", player.getId(), null, null, null, null, null,
                player.name() + " spent a chip to skip", true);
        board.advanceToNextEligible();
        state = State.WAITING_FOR_ROLL;
    }

    public boolean canResolveCasino(int number) {
        return state == State.RESOLVING && number >= 1 && number <= 6;
    }

    public void resolveCasino(int number) {
        LasVegasCasino casino = casino(number);
        var influenceGroups = new HashMap<Integer, List<String>>();
        for (var entry : casino.placements().entrySet()) {
            int influence = entry.getValue().influence();
            if (influence > 0) influenceGroups.computeIfAbsent(influence, ignored -> new ArrayList<>()).add(entry.getKey());
        }
        Set<String> eliminated = new HashSet<>();
        influenceGroups.values().stream().filter(group -> group.size() > 1).forEach(eliminated::addAll);
        var eligible = casino.placements().entrySet().stream()
                .filter(entry -> entry.getValue().influence() > 0 && !eliminated.contains(entry.getKey()))
                .sorted(Map.Entry.<String, LasVegasCasino.Placement>comparingByValue(
                        Comparator.comparingInt(LasVegasCasino.Placement::influence)
                ).reversed())
                .toList();

        var bonuses = casino.bonuses();
        int awarded = Math.min(2, eligible.size());
        for (int index = 0; index < awarded; index++) {
            String winnerId = eligible.get(index).getKey();
            LasVegasPlayer winner = player(winnerId);
            LasVegasMoneyCard prize = bonuses.get(index);
            winner.award(prize);
            if (index == 0) jackpotWinners.put(number, winnerId);
            String prizeName = index == 0 ? "jackpot" : "second prize";
            appendEvent(
                    index == 0 ? "CASINO_JACKPOT" : "CASINO_SECOND_PRIZE",
                    winnerId,
                    number,
                    null,
                    null,
                    null,
                    prize.amount(),
                    winner.name() + " won casino " + number + " " + prizeName,
                    true
            );
        }
        for (int index = awarded; index < bonuses.size(); index++) deck.putBottom(bonuses.get(index));
        if (eligible.isEmpty()) {
            appendEvent("CASINO_NO_WINNER", null, number, null, null, null, null,
                    "Casino " + number + " had no winner", true);
        }
    }

    public boolean canAdvanceRound() {
        return state == State.RESOLVING && internalRound < TOTAL_ROUNDS;
    }

    public void advanceRound() {
        String nextStarter = null;
        for (int number = 6; number >= 1; number--) {
            if (jackpotWinners.containsKey(number)) {
                nextStarter = jackpotWinners.get(number);
                break;
            }
        }
        if (nextStarter == null) nextStarter = board.nextSeatId(roundStarterId);
        internalRound++;
        roundStarterId = nextStarter;
        board.moveToPlayer(nextStarter);
        board.seats().forEach(player -> {
            player.resetDice();
            player.addRoundChips();
        });
        casinos.forEach(LasVegasCasino::clearForNextRound);
        jackpotWinners.clear();
        currentRoll = List.of();
        dealCasinoBonuses();
        state = State.WAITING_FOR_ROLL;
        appendEvent("ROUND_STARTED", nextStarter, null, null, null, null, null,
                "Casino round " + internalRound + " started", true);
    }

    public boolean canEndGame() {
        return state == State.RESOLVING && internalRound == TOTAL_ROUNDS;
    }

    public void endGame() {
        var sorted = board.seats().stream()
                .sorted(Comparator.comparingInt(LasVegasPlayer::totalAssets).reversed()
                        .thenComparing(Comparator.comparingInt(LasVegasPlayer::tieBreakCount).reversed())
                        .thenComparing(LasVegasPlayer::getId))
                .toList();
        var finalResults = new ArrayList<ResultView>();
        int rank = 0;
        Integer lastAssets = null;
        Integer lastTieBreak = null;
        for (int index = 0; index < sorted.size(); index++) {
            var player = sorted.get(index);
            if (lastAssets == null || player.totalAssets() != lastAssets || player.tieBreakCount() != lastTieBreak) {
                rank = index + 1;
            }
            finalResults.add(new ResultView(
                    player.getId(),
                    player.name(),
                    rank,
                    player.cashTotal(),
                    player.chips(),
                    player.totalAssets(),
                    player.tieBreakCount(),
                    rank == 1
            ));
            lastAssets = player.totalAssets();
            lastTieBreak = player.tieBreakCount();
        }
        results = List.copyOf(finalResults);
        endingPhase = new LasVegasEndingPhase(results);
        state = State.FINISHED;
        currentRoll = List.of();
        String winners = results.stream().filter(ResultView::winner).map(ResultView::name).reduce((a, b) -> a + ", " + b).orElse("No one");
        appendEvent("GAME_ENDED", null, null, null, null, null, null,
                winners + " won the game", true);
    }

    public synchronized GameView buildView(String viewerId, Map<String, Integer> presentedTotals) {
        player(viewerId); // Reject unknown perspectives.
        boolean finished = state == State.FINISHED;
        var playerViews = new ArrayList<PlayerView>();
        for (int index = 0; index < board.seats().size(); index++) {
            var player = board.seats().get(index);
            boolean privateValues = finished || player.getId().equals(viewerId);
            playerViews.add(new PlayerView(
                    player.getId(),
                    player.name(),
                    player.isBot(),
                    index,
                    player.getId().equals(board.currentPlayer().getId()),
                    player.remainingRegularDice(),
                    player.bigDieRemaining(),
                    player.remainingDiceCount(),
                    player.chips(),
                    player.moneyCardCount(),
                    privateValues ? player.moneyCards().stream().map(LasVegasMoneyCard::amount).toList() : null,
                    privateValues ? player.cashTotal() : null,
                    privateValues ? player.totalAssets() : null,
                    presentedTotals == null ? null : presentedTotals.get(player.getId())
            ));
        }
        var casinoViews = casinos.stream().map(casino -> {
            var placements = new ArrayList<PlacementView>();
            for (var player : board.seats()) {
                var placement = casino.placements().get(player.getId());
                if (placement != null) placements.add(new PlacementView(
                        player.getId(), placement.regularDice(), placement.bigDie(), placement.influence()
                ));
            }
            return new CasinoView(
                    casino.number(),
                    casino.bonuses().stream().map(LasVegasMoneyCard::amount).toList(),
                    List.copyOf(placements)
            );
        }).toList();
        return new GameView(
                SCHEMA_VERSION,
                state.name(),
                stateVersion,
                internalRound,
                TOTAL_ROUNDS,
                board.turnCount(),
                viewerId,
                board.currentPlayer().getId(),
                currentRoll.stream().map(roll -> new RollView(roll.face(), roll.big())).toList(),
                List.copyOf(playerViews),
                casinoViews,
                List.copyOf(actionLog),
                finished ? results : List.of()
        );
    }

    public synchronized LasVegasSnapshot snapshot() {
        var playerStates = board.seats().stream().map(player -> new LasVegasSnapshot.PlayerState(
                player.getId(),
                player.name(),
                player.isBot(),
                player.chips(),
                player.remainingRegularDice(),
                player.bigDieRemaining(),
                player.moneyCards().stream().map(LasVegasMoneyCard::amount).toList()
        )).toList();
        var casinoStates = casinos.stream().map(casino -> new LasVegasSnapshot.CasinoState(
                casino.number(),
                casino.jackpot().amount(),
                casino.secondPrize().amount(),
                casino.placements().entrySet().stream().map(entry -> new LasVegasSnapshot.PlacementState(
                        entry.getKey(), entry.getValue().regularDice(), entry.getValue().bigDie()
                )).toList(),
                jackpotWinners.get(casino.number())
        )).toList();
        return new LasVegasSnapshot(
                SCHEMA_VERSION,
                internalRound,
                state.name(),
                board.turnCount(),
                stateVersion,
                eventSequence,
                board.currentPlayer().getId(),
                roundStarterId,
                playerStates,
                deck.snapshotAmounts(),
                casinoStates,
                currentRoll.stream().map(roll -> new LasVegasSnapshot.RollState(roll.face(), roll.big())).toList(),
                List.copyOf(actionLog),
                results
        );
    }

    public synchronized Map<String, Integer> totalsFor(Set<String> visiblePlayerIds) {
        if (visiblePlayerIds == null || visiblePlayerIds.isEmpty()) return Map.of();
        var totals = new LinkedHashMap<String, Integer>();
        for (var player : board.seats()) {
            if (visiblePlayerIds.contains(player.getId())) totals.put(player.getId(), player.totalAssets());
        }
        return Map.copyOf(totals);
    }

    public synchronized int totalAssets(String playerId) { return player(playerId).totalAssets(); }
    public synchronized List<String> playerIds() { return board.seats().stream().map(LasVegasPlayer::getId).toList(); }
    public synchronized List<PublicEvent> drainPublicEvents() {
        var emitted = List.copyOf(transientEvents);
        transientEvents.clear();
        return emitted;
    }
    public synchronized long stateVersion() { return stateVersion; }
    public synchronized State state() { return state; }
    public synchronized int internalRound() { return internalRound; }
    public synchronized LasVegasEndingPhase endingPhase() { return endingPhase; }

    public synchronized String currentPlayerId() { return board.currentPlayer().getId(); }

    public synchronized boolean currentPlayerIsBot() { return board.currentPlayer().isBot(); }

    /** Public-information-only projection used by the server-side bot strategy. */
    public synchronized LasVegasBotStrategy.TurnState botTurnState() {
        LasVegasPlayer bot = board.currentPlayer();
        if (!bot.isBot()) throw new IllegalStateException("current player is not a bot");
        var casinoStates = casinos.stream().map(casino -> new LasVegasBotStrategy.CasinoState(
                casino.number(),
                casino.bonuses().stream().map(LasVegasMoneyCard::amount).toList(),
                casino.placements().entrySet().stream().map(entry -> new LasVegasBotStrategy.PlacementState(
                        entry.getKey(), entry.getValue().influence()
                )).toList()
        )).toList();
        var playerStates = board.seats().stream().map(player -> new LasVegasBotStrategy.PlayerState(
                player.getId(), player.remainingDiceCount(), player.chips()
        )).toList();
        return new LasVegasBotStrategy.TurnState(
                bot.getId(),
                bot.chips(),
                currentRoll.stream().map(roll -> new LasVegasBotStrategy.DieState(roll.face(), roll.big())).toList(),
                casinoStates,
                playerStates
        );
    }

    public synchronized LasVegasBotTicket botTicket(String gameId) {
        if (state == State.FINISHED || !board.currentPlayer().isBot()) return null;
        if (state != State.WAITING_FOR_ROLL && state != State.WAITING_FOR_CHOICE) return null;
        return new LasVegasBotTicket(gameId, stateVersion, state, board.currentPlayer().getId());
    }

    public synchronized boolean isLegalBotDecision(LasVegasBotStrategy.Decision decision) {
        if (decision == null || !board.currentPlayer().isBot()) return false;
        LasVegasPlayer bot = board.currentPlayer();
        return switch (decision.type()) {
            case "PLACE_DICE" -> decision.face() != null && canPlace(bot, decision.face());
            case "SKIP_TURN" -> decision.face() == null && canSkip(bot);
            default -> false;
        };
    }

    public synchronized int lowestLegalFace() {
        return currentRoll.stream().mapToInt(LasVegasDieRoll::face).min()
                .orElseThrow(() -> new IllegalStateException("current roll is empty"));
    }

    private LasVegasPlayer player(String playerId) {
        return board.seats().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("player not found"));
    }

    private boolean isCurrent(LasVegasPlayer player) {
        return player != null && board.currentPlayer().getId().equals(player.getId());
    }

    private LasVegasCasino casino(int number) {
        if (number < 1 || number > casinos.size()) throw new IllegalArgumentException("casino number must be 1-6");
        return casinos.get(number - 1);
    }

    private void dealCasinoBonuses() {
        record Pair(LasVegasMoneyCard first, LasVegasMoneyCard second, int order) {
            int total() { return first.amount() + second.amount(); }
            int highest() { return Math.max(first.amount(), second.amount()); }
        }
        var pairs = new ArrayList<Pair>(6);
        for (int index = 0; index < 6; index++) {
            LasVegasMoneyCard first = deck.draw();
            LasVegasMoneyCard second = deck.draw();
            if (first == null || second == null) throw new IllegalStateException("money deck exhausted");
            pairs.add(new Pair(first, second, index));
        }
        pairs.sort(Comparator.comparingInt(Pair::total)
                .thenComparingInt(Pair::highest)
                .thenComparingInt(Pair::order));
        for (int index = 0; index < casinos.size(); index++) {
            Pair pair = pairs.get(index);
            casinos.get(index).setBonuses(pair.first(), pair.second());
        }
    }

    private void appendEvent(
            String type,
            String actorId,
            Integer casinoNumber,
            Integer face,
            Integer regularDice,
            Boolean bigDie,
            Integer amount,
            String text,
            boolean persistRedacted
    ) {
        long sequence = ++eventSequence;
        Instant now = Instant.now();
        transientEvents.add(new PublicEvent(
                sequence, type, actorId, casinoNumber, face, regularDice, bigDie, amount, null, text, now
        ));
        if (persistRedacted) {
            actionLog.add(new ActionLogEntry(sequence, type, actorId, casinoNumber, text, now));
        }
    }
}
