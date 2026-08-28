package com.flip.backend.conquerwesteros.engine.phase;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.bot.ConquerWesterosBotStrategy;
import com.flip.backend.conquerwesteros.bot.ConquerWesterosBotTicket;
import com.flip.backend.conquerwesteros.engine.Campaign;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosCatalog;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshot;
import com.flip.backend.conquerwesteros.engine.event.ConquerWesterosEvents;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ActionLogEntry;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.AttemptView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ClanView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.DieView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.LegalActionsView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.LineView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PlayerView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PublicEvent;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ResultView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.StrongholdView;
import com.flip.backend.conquerwesteros.entities.BattleLine;
import com.flip.backend.conquerwesteros.entities.ConquerWesterosBoard;
import com.flip.backend.conquerwesteros.entities.ConquerWesterosPlayer;
import com.flip.backend.conquerwesteros.entities.DieFace;
import com.flip.backend.conquerwesteros.entities.StrongholdCard;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.game.engine.phase.RuntimePhase;
import com.flip.backend.security.GameStateConflictException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Event-driven aggregate for one complete Conquer Westeros game. */
public final class ConquerWesterosRuntimePhase extends RuntimePhase {
    public static final int SCHEMA_VERSION = 2;
    public static final int DICE_COUNT = 7;
    private static final int MAX_ACTION_LOG = 200;

    public enum State { WAITING_FOR_ROLL, WAITING_FOR_DECISION, RESOLVING, FINISHED }

    public record Command(
            long expectedVersion,
            String type,
            String targetId,
            String lineId,
            List<Integer> dieIds,
            Integer dieId
    ) {
        public Command {
            dieIds = dieIds == null ? List.of() : List.copyOf(dieIds);
        }
    }

    public record CommandBatch(List<PublicEvent> publicEvents) {}

    private static final class Attempt {
        String targetId;
        String targetOwnerId;
        boolean stealing;
        final Map<String, List<Integer>> committedLines = new LinkedHashMap<>();
        final Set<Integer> lostDieIds = new LinkedHashSet<>();

        boolean started() { return targetId != null; }
        Set<Integer> committedDieIds() {
            var result = new LinkedHashSet<Integer>();
            committedLines.values().forEach(result::addAll);
            return result;
        }
    }

    private final Random random;
    private final Campaign campaign;
    private final ConquerWesterosCatalog.CampaignData catalog;
    private final ConquerWesterosBoard board;
    private final Set<String> centralStrongholds = new LinkedHashSet<>();
    private final List<ActionLogEntry> actionLog = new ArrayList<>();
    private final List<PublicEvent> transientEvents = new ArrayList<>();

    private State state = State.WAITING_FOR_ROLL;
    private long stateVersion;
    private long eventSequence;
    private String ironThroneHolderId;
    private Attempt attempt = new Attempt();
    private Map<Integer, DieFace> currentRoll = Map.of();
    private List<ResultView> results = List.of();
    private ConquerWesterosEndingPhase endingPhase;

    private ConquerWesterosRuntimePhase(
            Campaign campaign,
            ConquerWesterosBoard board,
            Random random
    ) {
        this.campaign = Objects.requireNonNull(campaign, "campaign");
        this.catalog = ConquerWesterosCatalog.campaign(campaign);
        this.board = Objects.requireNonNull(board, "board");
        this.random = Objects.requireNonNull(random, "random");
    }

    public static ConquerWesterosRuntimePhase newGame(
            List<PlayerStartInfo> playerInfos,
            Campaign campaign,
            Random random
    ) {
        Objects.requireNonNull(playerInfos, "playerInfos");
        if (playerInfos.size() < 2 || playerInfos.size() > 6) {
            throw new IllegalArgumentException("Conquer Westeros requires 2-6 players");
        }
        var players = playerInfos.stream()
                .map(info -> new ConquerWesterosPlayer(info.playerId(), info.name(), info.bot()))
                .toList();
        var board = new ConquerWesterosBoard(players);
        board.moveToPlayer(players.get(random.nextInt(players.size())).getId());
        var runtime = new ConquerWesterosRuntimePhase(campaign, board, random);
        runtime.centralStrongholds.addAll(runtime.catalog.strongholds().stream().map(StrongholdCard::id).toList());
        runtime.appendEvent("GAME_STARTED", null, null,
                campaign.display() + " began", true, null, null, null);
        runtime.appendEvent("TURN_STARTED", board.currentPlayer().getId(), null,
                board.currentPlayer().name() + " is ready to roll", true, null, null, null);
        return runtime;
    }

    public static ConquerWesterosRuntimePhase restore(ConquerWesterosSnapshot snapshot) {
        return restore(snapshot, new SecureRandom());
    }

    public static ConquerWesterosRuntimePhase restore(ConquerWesterosSnapshot snapshot, Random random) {
        if (snapshot == null || (snapshot.schemaVersion() != 1 && snapshot.schemaVersion() != SCHEMA_VERSION)) {
            throw new IllegalArgumentException("unsupported Conquer Westeros snapshot schema");
        }
        Campaign campaign = Campaign.parse(snapshot.campaign());
        var players = new ArrayList<ConquerWesterosPlayer>();
        for (var saved : snapshot.players()) {
            var player = new ConquerWesterosPlayer(saved.playerId(), saved.name(), saved.bot());
            player.restore(new LinkedHashSet<>(saved.faceUpStrongholds()), saved.completedClans());
            players.add(player);
        }
        var board = new ConquerWesterosBoard(players);
        board.restoreCurrentPlayer(snapshot.currentPlayerId(), snapshot.turnCount());
        var runtime = new ConquerWesterosRuntimePhase(campaign, board, random);
        runtime.state = State.valueOf(snapshot.phase());
        runtime.stateVersion = snapshot.stateVersion();
        runtime.eventSequence = snapshot.eventSequence();
        runtime.ironThroneHolderId = snapshot.ironThroneHolderId();
        runtime.centralStrongholds.addAll(snapshot.centralStrongholds());
        if (snapshot.attempt() != null) {
            runtime.attempt.targetId = snapshot.attempt().targetId();
            runtime.attempt.targetOwnerId = snapshot.attempt().targetOwnerId();
            runtime.attempt.stealing = snapshot.attempt().stealing();
            snapshot.attempt().committedLines().forEach((id, dice) ->
                    runtime.attempt.committedLines.put(id, List.copyOf(dice)));
            runtime.attempt.lostDieIds.addAll(snapshot.attempt().lostDieIds());
        }
        var roll = new LinkedHashMap<Integer, DieFace>();
        snapshot.currentRoll().forEach(saved -> roll.put(saved.dieId(), DieFace.valueOf(saved.face())));
        runtime.currentRoll = Map.copyOf(roll);
        runtime.actionLog.addAll(snapshot.actionLog());
        runtime.results = List.copyOf(snapshot.results());
        if (runtime.state == State.FINISHED) runtime.endingPhase = new ConquerWesterosEndingPhase(runtime.results);
        runtime.validateRestoredOwnership();
        return runtime;
    }

    @Override public void enter() { }
    @Override public String run() { return state.name(); }

    public synchronized CommandBatch applyCommand(String playerId, Command command) {
        Objects.requireNonNull(command, "command");
        if (command.expectedVersion() != stateVersion) {
            throw new GameStateConflictException("expected version does not match current state");
        }
        ConquerWesterosPlayer player = player(playerId);
        EventQueue queue = new EventQueue();
        String type = command.type() == null ? "" : command.type().trim().toUpperCase();
        switch (type) {
            case "ROLL_DICE" -> queue.enqueue(new ConquerWesterosEvents.Roll(this, player));
            case "COMPLETE_LINE" -> queue.enqueue(new ConquerWesterosEvents.CompleteLine(
                    this, player, command.targetId(), command.lineId(), command.dieIds(), queue));
            case "LOSE_DIE" -> {
                if (command.dieId() == null) throw new IllegalArgumentException("dieId is required");
                queue.enqueue(new ConquerWesterosEvents.LoseDie(this, player, command.dieId(), queue));
            }
            default -> throw new IllegalArgumentException("unsupported Conquer Westeros command");
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

    public boolean canRoll(ConquerWesterosPlayer player) {
        return isCurrent(player) && state == State.WAITING_FOR_ROLL && !activeDieIds().isEmpty();
    }

    public boolean canCompleteLine(
            ConquerWesterosPlayer player,
            String targetId,
            String lineId,
            List<Integer> dieIds
    ) {
        if (!isCurrent(player) || state != State.WAITING_FOR_DECISION || targetId == null || lineId == null) return false;
        if (dieIds == null || dieIds.isEmpty() || new LinkedHashSet<>(dieIds).size() != dieIds.size()) return false;
        if (attempt.started() && !attempt.targetId.equals(targetId)) return false;
        Target target = resolveTarget(player, targetId);
        if (target == null) return false;
        boolean stealing = target.owner() != null;
        BattleLine line = requiredLines(target.card(), attempt.started() ? attempt.stealing : stealing).stream()
                .filter(candidate -> candidate.id().equals(lineId))
                .findFirst().orElse(null);
        if (line == null || attempt.committedLines.containsKey(lineId)) return false;
        if (dieIds.stream().anyMatch(id -> !currentRoll.containsKey(id))) return false;
        return line.matches(dieIds.stream().map(currentRoll::get).toList());
    }

    public boolean canLoseDie(ConquerWesterosPlayer player, int dieId) {
        return isCurrent(player) && state == State.WAITING_FOR_DECISION && currentRoll.containsKey(dieId);
    }

    public void rollDice(ConquerWesterosPlayer player) {
        var roll = new LinkedHashMap<Integer, DieFace>();
        for (int id : activeDieIds()) roll.put(id, DieFace.values()[random.nextInt(DieFace.values().length)]);
        currentRoll = Map.copyOf(roll);
        state = State.WAITING_FOR_DECISION;
        appendEvent("ROLL_DICE", player.getId(), attempt.targetId,
                player.name() + " rolled " + roll.size() + " dice", true, null, null, new ArrayList<>(roll.keySet()));
    }

    public void completeLine(
            ConquerWesterosPlayer player,
            String targetId,
            String lineId,
            List<Integer> dieIds,
            EventQueue queue
    ) {
        if (!attempt.started()) {
            Target target = Objects.requireNonNull(resolveTarget(player, targetId));
            attempt.targetId = targetId;
            attempt.targetOwnerId = target.owner() == null ? null : target.owner().getId();
            attempt.stealing = target.owner() != null;
        }
        attempt.committedLines.put(lineId, List.copyOf(dieIds));
        currentRoll = Map.of();
        appendEvent("LINE_COMPLETED", player.getId(), targetId,
                player.name() + " completed " + lineId + " at " + catalog.stronghold(targetId).name(),
                true, lineId, null, dieIds);
        if (allLinesCompleted()) {
            state = State.RESOLVING;
            queue.enqueue(new ConquerWesterosEvents.Capture(this, player, queue));
        } else if (activeDieIds().isEmpty()) {
            state = State.RESOLVING;
            queue.enqueue(new ConquerWesterosEvents.SiegeFailed(this, player, queue));
        } else {
            state = State.WAITING_FOR_ROLL;
        }
    }

    public void loseDie(ConquerWesterosPlayer player, int dieId, EventQueue queue) {
        attempt.lostDieIds.add(dieId);
        currentRoll = Map.of();
        appendEvent("DIE_LOST", player.getId(), attempt.targetId,
                player.name() + " lost die " + dieId, true, null, dieId, List.of(dieId));
        if (activeDieIds().isEmpty()) {
            state = State.RESOLVING;
            queue.enqueue(new ConquerWesterosEvents.SiegeFailed(this, player, queue));
        } else {
            state = State.WAITING_FOR_ROLL;
        }
    }

    public boolean canFailSiege(ConquerWesterosPlayer player) {
        return isCurrent(player) && state == State.RESOLVING && !allLinesCompleted() && activeDieIds().isEmpty();
    }

    public void failSiege(ConquerWesterosPlayer player, EventQueue queue) {
        appendEvent("SIEGE_FAILED", player.getId(), attempt.targetId,
                player.name() + " failed the siege", true, null, null, null);
        queue.enqueue(new ConquerWesterosEvents.AdvanceTurn(this, player));
    }

    public boolean canCapture(ConquerWesterosPlayer player) {
        return isCurrent(player) && state == State.RESOLVING && attempt.started() && allLinesCompleted();
    }

    public void capture(ConquerWesterosPlayer player, EventQueue queue) {
        StrongholdCard card = catalog.stronghold(attempt.targetId);
        boolean transferForTheft = attempt.stealing
                && ironThroneHolderId != null
                && ironThroneHolderId.equals(attempt.targetOwnerId);
        if (attempt.stealing) {
            ConquerWesterosPlayer previous = player(attempt.targetOwnerId);
            previous.removeFaceUp(card.id());
        } else if (!centralStrongholds.remove(card.id())) {
            throw new IllegalStateException("central stronghold is unavailable");
        }
        player.addFaceUp(card.id());
        appendEvent(attempt.stealing ? "STRONGHOLD_STOLEN" : "STRONGHOLD_CAPTURED", player.getId(), card.id(),
                player.name() + (attempt.stealing ? " stole " : " captured ") + card.name(),
                true, null, null, null);

        if (card.kingsLanding()) {
            queue.enqueue(new ConquerWesterosEvents.TransferIronThrone(this, player, "King's Landing changed hands"));
        } else if (transferForTheft) {
            queue.enqueue(new ConquerWesterosEvents.TransferIronThrone(this, player, "a stronghold was stolen from the throne holder"));
        }
        if (canCompleteClan(player, card.clan())) {
            queue.enqueue(new ConquerWesterosEvents.CompleteClan(this, player, card.clan()));
        }
        if (centralStrongholds.isEmpty()) queue.enqueue(new ConquerWesterosEvents.EndGame(this, player));
        else queue.enqueue(new ConquerWesterosEvents.AdvanceTurn(this, player));
    }

    public boolean canTransferIronThrone(ConquerWesterosPlayer player) {
        return isCurrent(player) && state == State.RESOLVING;
    }

    public void transferIronThrone(ConquerWesterosPlayer player, String reason) {
        ironThroneHolderId = player.getId();
        appendEvent("IRON_THRONE_TRANSFERRED", player.getId(), attempt.targetId,
                player.name() + " took the Iron Throne: " + reason, true, null, null, null);
    }

    public boolean canCompleteClan(ConquerWesterosPlayer player, String clan) {
        if (!isCurrent(player) || state != State.RESOLVING || clan == null || player.completedClans().containsKey(clan)) return false;
        List<String> required = catalog.clanStrongholds(clan);
        return !required.isEmpty() && required.stream().allMatch(player::ownsFaceUp);
    }

    public void completeClan(ConquerWesterosPlayer player, String clan) {
        List<String> cards = catalog.clanStrongholds(clan);
        player.completeClan(clan, cards);
        appendEvent("CLAN_COMPLETED", player.getId(), attempt.targetId,
                player.name() + " completed " + clan, true, null, null, null);
    }

    public boolean canAdvanceTurn() {
        return state == State.RESOLVING && !centralStrongholds.isEmpty();
    }

    public void advanceTurn() {
        board.tickTurn();
        board.step(1);
        attempt = new Attempt();
        currentRoll = Map.of();
        state = State.WAITING_FOR_ROLL;
        appendEvent("TURN_STARTED", board.currentPlayer().getId(), null,
                board.currentPlayer().name() + " is ready to roll", true, null, null, null);
    }

    public boolean canEndGame() { return state == State.RESOLVING && centralStrongholds.isEmpty(); }

    public void endGame() {
        board.tickTurn();
        var scored = board.seats().stream().map(this::score).sorted(
                Comparator.comparingInt(Score::totalScore).reversed()
                        .thenComparing(Comparator.comparingInt(Score::thronePoint).reversed())
                        .thenComparing(Comparator.comparingInt(Score::strongholdCount).reversed())
                        .thenComparing(Comparator.comparingInt(Score::completedClanCount).reversed())
                        .thenComparing(Score::playerId)
        ).toList();
        var finalResults = new ArrayList<ResultView>();
        int rank = 0;
        Score previous = null;
        for (int index = 0; index < scored.size(); index++) {
            Score score = scored.get(index);
            if (previous == null || !score.sameRank(previous)) rank = index + 1;
            finalResults.add(new ResultView(
                    score.playerId(), score.name(), rank, score.totalScore(), score.faceUpScore(), score.clanScore(),
                    score.thronePoint(), score.strongholdCount(), score.completedClanCount(), rank == 1));
            previous = score;
        }
        results = List.copyOf(finalResults);
        endingPhase = new ConquerWesterosEndingPhase(results);
        attempt = new Attempt();
        currentRoll = Map.of();
        state = State.FINISHED;
        String winners = results.stream().filter(ResultView::winner).map(ResultView::name)
                .reduce((left, right) -> left + ", " + right).orElse("No one");
        appendEvent("GAME_ENDED", null, null, winners + " won Conquer Westeros", true, null, null, null);
    }

    public synchronized GameView buildView(String viewerId) {
        player(viewerId);
        var players = new ArrayList<PlayerView>();
        for (int index = 0; index < board.seats().size(); index++) {
            var player = board.seats().get(index);
            Score score = score(player);
            var clans = player.completedClans().entrySet().stream()
                    .map(entry -> new ClanView(entry.getKey(), catalog.clanScore(entry.getKey()), entry.getValue()))
                    .toList();
            players.add(new PlayerView(
                    player.getId(), player.name(), player.isBot(), index, isCurrent(player), player.getId().equals(ironThroneHolderId),
                    player.faceUpStrongholds().stream().sorted().toList(), clans, score.strongholdCount(),
                    score.completedClanCount(), score.faceUpScore(), score.clanScore(), score.totalScore()));
        }
        boolean viewerCurrent = viewerId.equals(board.currentPlayer().getId()) && state != State.FINISHED;
        var legalTargets = viewerCurrent && state == State.WAITING_FOR_DECISION
                ? attempt.started()
                    ? List.of(attempt.targetId)
                    : legalTargets(board.currentPlayer()).stream().map(target -> target.card().id()).toList()
                : List.<String>of();
        var legalDice = viewerCurrent && state == State.WAITING_FOR_DECISION
                ? currentRoll.keySet().stream().sorted().toList() : List.<Integer>of();
        var legal = new LegalActionsView(
                viewerCurrent && state == State.WAITING_FOR_ROLL,
                viewerCurrent && state == State.WAITING_FOR_DECISION,
                viewerCurrent && state == State.WAITING_FOR_DECISION,
                legalTargets,
                legalDice);
        return new GameView(
                SCHEMA_VERSION, state.name(), stateVersion, campaign.name(), campaign.display(), board.turnCount(),
                viewerId, board.currentPlayer().getId(), ironThroneHolderId,
                currentRoll.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry ->
                        new DieView(entry.getKey(), entry.getValue().name(), entry.getValue().militaryStrength(), entry.getValue().display())).toList(),
                buildAttemptView(), List.copyOf(players), buildStrongholdViews(), legal,
                List.copyOf(actionLog), results);
    }

    public ConquerWesterosSnapshot snapshot() {
        return new ConquerWesterosSnapshot(
                SCHEMA_VERSION, campaign.name(), state.name(), board.turnCount(), stateVersion, eventSequence,
                board.currentPlayer().getId(), ironThroneHolderId, List.copyOf(centralStrongholds),
                board.seats().stream().map(player -> new ConquerWesterosSnapshot.PlayerState(
                        player.getId(), player.name(), player.isBot(), player.faceUpStrongholds().stream().sorted().toList(), player.completedClans())).toList(),
                new ConquerWesterosSnapshot.AttemptState(
                        attempt.targetId, attempt.targetOwnerId, attempt.stealing,
                        Map.copyOf(attempt.committedLines), List.copyOf(attempt.lostDieIds)),
                currentRoll.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(entry -> new ConquerWesterosSnapshot.RollState(entry.getKey(), entry.getValue().name())).toList(),
                List.copyOf(actionLog), results);
    }

    public List<PublicEvent> drainPublicEvents() {
        var result = List.copyOf(transientEvents);
        transientEvents.clear();
        return result;
    }

    public State state() { return state; }
    public long stateVersion() { return stateVersion; }
    public Campaign campaign() { return campaign; }
    public String currentPlayerId() { return board.currentPlayer().getId(); }
    public List<String> playerIds() { return board.seats().stream().map(ConquerWesterosPlayer::getId).toList(); }
    public ConquerWesterosEndingPhase endingPhase() { return endingPhase; }

    public synchronized boolean currentPlayerIsBot() { return board.currentPlayer().isBot(); }

    public synchronized ConquerWesterosBotTicket botTicket(String gameId) {
        if (!board.currentPlayer().isBot()
                || (state != State.WAITING_FOR_ROLL && state != State.WAITING_FOR_DECISION)) return null;
        return new ConquerWesterosBotTicket(gameId, stateVersion, state, board.currentPlayer().getId());
    }

    /** Public-information-only projection for the server-side Bot strategy. */
    public synchronized ConquerWesterosBotStrategy.TurnState botTurnState() {
        ConquerWesterosPlayer bot = board.currentPlayer();
        if (!bot.isBot() || state != State.WAITING_FOR_DECISION || currentRoll.isEmpty()) {
            throw new IllegalStateException("current player is not a Bot waiting for a decision");
        }
        List<Target> available = attempt.started()
                ? List.of(Objects.requireNonNull(resolveTarget(bot, attempt.targetId)))
                : legalTargets(bot);
        var targetStates = available.stream().map(target -> {
            StrongholdCard card = target.card();
            int ownedCount = (int) catalog.clanStrongholds(card.clan()).stream().filter(bot::ownsFaceUp).count();
            int ownedPoints = catalog.clanStrongholds(card.clan()).stream().filter(bot::ownsFaceUp)
                    .map(catalog::stronghold).mapToInt(StrongholdCard::points).sum();
            var remainingLines = requiredLines(card, target.owner() != null).stream()
                    .filter(line -> !attempt.committedLines.containsKey(line.id()))
                    .map(this::botLineState)
                    .toList();
            return new ConquerWesterosBotStrategy.TargetState(
                    card.id(), card.points(), catalog.clanScore(card.clan()), catalog.clanStrongholds(card.clan()).size(),
                    ownedCount, ownedPoints, card.kingsLanding(),
                    target.owner() == null ? null : target.owner().getId(), target.owner() == null, remainingLines);
        }).toList();
        var dice = currentRoll.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new ConquerWesterosBotStrategy.DieState(
                        entry.getKey(), entry.getValue().name(), entry.getValue().militaryStrength()))
                .toList();
        return new ConquerWesterosBotStrategy.TurnState(
                bot.getId(), ironThroneHolderId, attempt.started(), dice, targetStates);
    }

    public synchronized boolean isLegalBotDecision(ConquerWesterosBotStrategy.Decision decision) {
        if (decision == null || !board.currentPlayer().isBot() || state != State.WAITING_FOR_DECISION) return false;
        return switch (decision.type()) {
            case "COMPLETE_LINE" -> decision.dieId() == null
                    && canCompleteLine(board.currentPlayer(), decision.targetId(), decision.lineId(), decision.dieIds());
            case "LOSE_DIE" -> decision.dieId() != null
                    && decision.targetId() == null && decision.lineId() == null && decision.dieIds().isEmpty()
                    && canLoseDie(board.currentPlayer(), decision.dieId());
            default -> false;
        };
    }

    private ConquerWesterosBotStrategy.LineState botLineState(BattleLine line) {
        if (line instanceof BattleLine.Military military) {
            return new ConquerWesterosBotStrategy.LineState(line.id(), "MILITARY", military.threshold(), List.of());
        }
        var symbols = (BattleLine.Symbols) line;
        return new ConquerWesterosBotStrategy.LineState(line.id(),
                ConquerWesterosCatalog.STEAL_CROWN_LINE_ID.equals(line.id()) ? "STEAL_CROWN" : "SYMBOLS",
                null, symbols.required().stream().map(DieFace::name).toList());
    }

    private AttemptView buildAttemptView() {
        if (!attempt.started()) {
            return new AttemptView(null, null, false, List.of(), List.copyOf(attempt.lostDieIds), List.of(), List.of());
        }
        StrongholdCard card = catalog.stronghold(attempt.targetId);
        var lines = requiredLines(card, attempt.stealing).stream().map(line -> lineView(
                line, attempt.committedLines.containsKey(line.id()), ConquerWesterosCatalog.STEAL_CROWN_LINE_ID.equals(line.id()))).toList();
        return new AttemptView(attempt.targetId, attempt.targetOwnerId, attempt.stealing,
                List.copyOf(attempt.committedLines.keySet()), List.copyOf(attempt.lostDieIds),
                attempt.committedDieIds().stream().sorted().toList(), lines);
    }

    private List<StrongholdView> buildStrongholdViews() {
        return catalog.strongholds().stream().map(card -> {
            String ownerId = null;
            boolean locked = false;
            for (var player : board.seats()) {
                if (player.ownsFaceUp(card.id())) ownerId = player.getId();
                if (player.ownsLocked(card.id())) {
                    ownerId = player.getId();
                    locked = true;
                }
            }
            final String resolvedOwner = ownerId;
            final boolean resolvedLocked = locked;
            boolean active = attempt.started() && attempt.targetId.equals(card.id());
            var lines = new ArrayList<LineView>();
            card.lines().forEach(line -> lines.add(lineView(line,
                    active && attempt.committedLines.containsKey(line.id()), false)));
            if (active && attempt.stealing) lines.add(lineView(ConquerWesterosCatalog.STEAL_CROWN_LINE,
                    attempt.committedLines.containsKey(ConquerWesterosCatalog.STEAL_CROWN_LINE_ID), true));
            return new StrongholdView(card.id(), card.name(), card.clan(), card.points(), card.kingsLanding(),
                    resolvedOwner, centralStrongholds.contains(card.id()), resolvedLocked, active && attempt.stealing, List.copyOf(lines));
        }).toList();
    }

    private LineView lineView(BattleLine line, boolean completed, boolean special) {
        if (line instanceof BattleLine.Military military) {
            return new LineView(line.id(), "MILITARY", military.threshold(), List.of(), line.display(), completed, special);
        }
        var symbols = (BattleLine.Symbols) line;
        return new LineView(line.id(), special ? "STEAL_CROWN" : "SYMBOLS", null,
                symbols.required().stream().map(DieFace::name).toList(), line.display(), completed, special);
    }

    private record Target(StrongholdCard card, ConquerWesterosPlayer owner) {}

    private Target resolveTarget(ConquerWesterosPlayer attacker, String targetId) {
        StrongholdCard card;
        try { card = catalog.stronghold(targetId); }
        catch (IllegalArgumentException exception) { return null; }
        if (centralStrongholds.contains(targetId)) return new Target(card, null);
        for (var candidate : board.seats()) {
            if (!candidate.equals(attacker) && candidate.ownsFaceUp(targetId)) return new Target(card, candidate);
        }
        return null;
    }

    private List<Target> legalTargets(ConquerWesterosPlayer attacker) {
        var result = new ArrayList<Target>();
        for (var card : catalog.strongholds()) {
            Target target = resolveTarget(attacker, card.id());
            if (target != null) result.add(target);
        }
        return List.copyOf(result);
    }

    private List<BattleLine> requiredLines(StrongholdCard card, boolean stealing) {
        if (!stealing) return card.lines();
        var result = new ArrayList<>(card.lines());
        result.add(ConquerWesterosCatalog.STEAL_CROWN_LINE);
        return List.copyOf(result);
    }

    private boolean allLinesCompleted() {
        if (!attempt.started()) return false;
        return requiredLines(catalog.stronghold(attempt.targetId), attempt.stealing).stream()
                .allMatch(line -> attempt.committedLines.containsKey(line.id()));
    }

    private List<Integer> activeDieIds() {
        Set<Integer> unavailable = new LinkedHashSet<>(attempt.lostDieIds);
        unavailable.addAll(attempt.committedDieIds());
        var result = new ArrayList<Integer>();
        for (int id = 0; id < DICE_COUNT; id++) if (!unavailable.contains(id)) result.add(id);
        return List.copyOf(result);
    }

    private boolean isCurrent(ConquerWesterosPlayer player) {
        return player != null && board.currentPlayer().equals(player);
    }

    private ConquerWesterosPlayer player(String playerId) {
        return board.seats().stream().filter(player -> player.getId().equals(playerId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown player"));
    }

    private record Score(
            String playerId,
            String name,
            int totalScore,
            int faceUpScore,
            int clanScore,
            int thronePoint,
            int strongholdCount,
            int completedClanCount
    ) {
        boolean sameRank(Score other) {
            return totalScore == other.totalScore
                    && thronePoint == other.thronePoint
                    && strongholdCount == other.strongholdCount
                    && completedClanCount == other.completedClanCount;
        }
    }

    private Score score(ConquerWesterosPlayer player) {
        int faceUp = player.faceUpStrongholds().stream().map(catalog::stronghold).mapToInt(StrongholdCard::points).sum();
        int clans = player.completedClans().keySet().stream().mapToInt(catalog::clanScore).sum();
        int throne = player.getId().equals(ironThroneHolderId) ? 1 : 0;
        return new Score(player.getId(), player.name(), faceUp + clans + throne, faceUp, clans, throne,
                player.strongholdCount(), player.completedClans().size());
    }

    private void appendEvent(
            String type,
            String actorId,
            String targetId,
            String text,
            boolean publicEvent,
            String lineId,
            Integer dieId,
            List<Integer> dieIds
    ) {
        long sequence = ++eventSequence;
        Instant now = Instant.now();
        actionLog.add(new ActionLogEntry(sequence, type, actorId, targetId, text, now));
        while (actionLog.size() > MAX_ACTION_LOG) actionLog.remove(0);
        if (publicEvent) transientEvents.add(new PublicEvent(
                sequence, type, actorId, targetId, lineId, dieId,
                dieIds == null ? List.of() : List.copyOf(dieIds), text, now));
    }

    private void validateRestoredOwnership() {
        var owned = new LinkedHashSet<String>();
        for (var player : board.seats()) {
            for (String id : player.allStrongholds()) {
                catalog.stronghold(id);
                if (!owned.add(id)) throw new IllegalArgumentException("snapshot stronghold is owned twice");
            }
        }
        for (String id : centralStrongholds) {
            catalog.stronghold(id);
            if (!owned.add(id)) throw new IllegalArgumentException("snapshot stronghold is both central and owned");
        }
        if (owned.size() != catalog.strongholds().size()) {
            throw new IllegalArgumentException("snapshot does not account for all strongholds");
        }
    }
}
