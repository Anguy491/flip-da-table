package com.flip.backend.lasvegas.engine;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.lasvegas.LasVegasPresentationService;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.engine.phase.LasVegasStartPhase;
import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LasVegasRuntimePhaseTest {
    @Test
    void validatesPlayerLimitsAndBots() {
        assertThrows(IllegalArgumentException.class, () -> LasVegasRuntimePhase.newGame(players(2), new ZeroRandom()));
        assertThrows(IllegalArgumentException.class, () -> LasVegasRuntimePhase.newGame(players(11), new ZeroRandom()));
        var withBot = new ArrayList<>(players(3));
        withBot.set(2, new PlayerStartInfo("BOT1", "Bot", true, true));
        assertThrows(IllegalArgumentException.class, () -> LasVegasRuntimePhase.newGame(withBot, new ZeroRandom()));

        assertEquals(3, LasVegasRuntimePhase.newGame(players(3), new ZeroRandom()).playerIds().size());
        assertEquals(10, LasVegasRuntimePhase.newGame(players(10), new ZeroRandom()).playerIds().size());
    }

    @Test
    void injectedSeedControlsTheRandomStarterDeckAndDice() {
        long seed = 1_234L;
        int expectedStarter = new Random(seed).nextInt(3) + 1;
        var first = LasVegasRuntimePhase.newGame(players(3), new Random(seed));
        var second = LasVegasRuntimePhase.newGame(players(3), new Random(seed));
        assertEquals("P" + expectedStarter, first.buildView("P1", Map.of()).currentPlayerId());
        assertEquals(first.snapshot().currentPlayerId(), second.snapshot().currentPlayerId());
        assertEquals(first.snapshot().deck(), second.snapshot().deck());
        assertEquals(first.snapshot().casinos(), second.snapshot().casinos());

        String starter = first.buildView("P1", Map.of()).currentPlayerId();
        first.applyCommand(starter, new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        second.applyCommand(starter, new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        assertEquals(first.buildView("P1", Map.of()).currentRoll(), second.buildView("P1", Map.of()).currentRoll());
    }

    @Test
    void rollsThenPlacesEveryMatchingRegularAndBigDie() {
        var runtime = runtime(3);
        assertEquals("P1", runtime.buildView("P1", Map.of()).currentPlayerId());

        runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        GameView rolled = runtime.buildView("P1", Map.of());
        assertEquals("WAITING_FOR_CHOICE", rolled.phase());
        assertEquals(8, rolled.currentRoll().size());
        assertTrue(rolled.currentRoll().stream().allMatch(die -> die.face() == 1));
        assertEquals(0, rolled.turnCount());
        assertThrows(com.flip.backend.security.GameStateConflictException.class, () ->
                runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(0, "PLACE_DICE", 1)));

        runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(1, "PLACE_DICE", 1));
        GameView placed = runtime.buildView("P1", Map.of());
        assertEquals("WAITING_FOR_ROLL", placed.phase());
        assertEquals("P2", placed.currentPlayerId());
        assertEquals(1, placed.turnCount());
        assertEquals(0, placed.players().get(0).remainingDice());
        assertEquals(7, placed.casinos().get(0).placements().get(0).regularDice());
        assertTrue(placed.casinos().get(0).placements().get(0).bigDie());
        assertEquals(9, placed.casinos().get(0).placements().get(0).influence());
    }

    @Test
    void skipSpendsOneChipAndKeepsAllDice() {
        var runtime = runtime(3);
        runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(1, "SKIP_TURN", null));

        GameView view = runtime.buildView("P1", Map.of());
        assertEquals(1, view.players().get(0).chips());
        assertEquals(8, view.players().get(0).remainingDice());
        assertEquals("P2", view.currentPlayerId());
        assertEquals(1, view.turnCount());
    }

    @Test
    void threeAllTieRoundsUseFallbackStartersAccumulateChipsAndShareVictory() {
        var runtime = runtime(3);

        playAllOnes(runtime, List.of("P1", "P2", "P3"));
        GameView roundTwo = runtime.buildView("P1", Map.of());
        assertEquals(2, roundTwo.internalRound());
        assertEquals("P2", roundTwo.currentPlayerId());
        assertTrue(roundTwo.players().stream().allMatch(player -> player.chips() == 4));

        playAllOnes(runtime, List.of("P2", "P3", "P1"));
        assertEquals("P3", runtime.buildView("P1", Map.of()).currentPlayerId());
        playAllOnes(runtime, List.of("P3", "P1", "P2"));

        GameView finished = runtime.buildView("P1", Map.of());
        assertEquals("FINISHED", finished.phase());
        assertEquals(3, finished.internalRound());
        assertEquals(3, finished.results().size());
        assertTrue(finished.results().stream().allMatch(result -> result.rank() == 1 && result.winner()));
        assertTrue(finished.results().stream().allMatch(result -> result.totalAssets() == 60_000));
    }

    @Test
    void eliminatesEveryTieGroupAwardsOnlyTwoAndHidesOpponentAmounts() {
        var runtime = LasVegasRuntimePhase.restore(resolvingSnapshot(), new ZeroRandom());
        runtime.resolveCasino(1);

        GameView p1 = runtime.buildView("P1", Map.of());
        GameView p2 = runtime.buildView("P2", Map.of());
        GameView p4 = runtime.buildView("P4", Map.of());
        assertEquals(100_000, p1.players().get(0).cashTotal());
        assertEquals(30_000, p4.players().get(3).cashTotal());
        assertEquals(0, p1.players().get(1).moneyCardCount());
        assertEquals(0, p1.players().get(2).moneyCardCount());
        assertNull(p2.players().get(0).cashTotal());
        assertNull(p2.players().get(0).moneyCards());
        assertEquals(1, p2.players().get(0).moneyCardCount());

        var publicEvents = runtime.drainPublicEvents();
        assertEquals(List.of(100_000, 30_000), publicEvents.stream()
                .filter(event -> event.amount() != null)
                .map(event -> event.amount())
                .toList());
        assertTrue(runtime.snapshot().actionLog().stream().noneMatch(entry -> entry.text().contains("100000")));
    }

    @Test
    void snapshotRestoresTheExactAggregateAndPresentationDoesNotChangeIt() {
        var runtime = runtime(3);
        runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        LasVegasSnapshot snapshot = runtime.snapshot();
        var restored = LasVegasRuntimePhase.restore(snapshot, new ZeroRandom());
        assertEquals(snapshot, restored.snapshot());

        var presentation = new LasVegasPresentationService();
        long version = restored.stateVersion();
        LasVegasSnapshot before = restored.snapshot();
        var event = presentation.setVisible("game", "P1", true, restored);
        assertEquals(20_000, event.amount());
        assertEquals(version, restored.stateVersion());
        assertEquals(before, restored.snapshot());
        assertEquals(20_000, presentation.totals("game", restored).get("P1"));
        presentation.setVisible("game", "P1", false, restored);
        assertTrue(presentation.totals("game", restored).isEmpty());
    }

    @Test
    void aSingleEligiblePlayerTakesOnlyTheJackpotAndReturnsTheSecondPrizeToDeckBottom() {
        LasVegasSnapshot base = resolvingSnapshot();
        var casinos = new ArrayList<>(base.casinos());
        casinos.set(0, new LasVegasSnapshot.CasinoState(1, 100_000, 30_000, List.of(
                new LasVegasSnapshot.PlacementState("P1", 2, false),
                new LasVegasSnapshot.PlacementState("P2", 1, false),
                new LasVegasSnapshot.PlacementState("P3", 1, false)
        ), null));
        var runtime = LasVegasRuntimePhase.restore(new LasVegasSnapshot(
                base.schemaVersion(), base.internalRound(), base.phase(), base.turnCount(), base.stateVersion(),
                base.eventSequence(), base.currentPlayerId(), base.roundStarterId(), base.players(),
                List.of(40_000), casinos, base.currentRoll(), base.actionLog(), base.results()
        ), new ZeroRandom());

        runtime.resolveCasino(1);

        assertEquals(100_000, runtime.buildView("P1", Map.of()).players().get(0).cashTotal());
        assertEquals(List.of(40_000, 30_000), runtime.snapshot().deck());
        assertEquals(1, runtime.drainPublicEvents().stream().filter(event -> event.amount() != null).count());
    }

    @Test
    void bonusPairsAreSortedFromLowestCasinoToHighestCasino() {
        GameView view = runtime(3).buildView("P1", Map.of());
        int previous = -1;
        for (var casino : view.casinos()) {
            assertEquals(2, casino.bonuses().size());
            assertTrue(casino.bonuses().get(0) >= casino.bonuses().get(1));
            int total = casino.bonuses().stream().mapToInt(Integer::intValue).sum();
            assertTrue(total >= previous);
            previous = total;
        }
    }

    @Test
    void finalRankingUsesCardPlusChipCountBeforeSharingVictory() {
        var players = List.of(
                new LasVegasSnapshot.PlayerState("P1", "Player 1", 0, 0, false, List.of(100_000)),
                new LasVegasSnapshot.PlayerState("P2", "Player 2", 2, 0, false, List.of(80_000)),
                new LasVegasSnapshot.PlayerState("P3", "Player 3", 0, 0, false, List.of(60_000))
        );
        var casinos = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(number -> new LasVegasSnapshot.CasinoState(number, 40_000, 30_000, List.of(), null))
                .toList();
        var runtime = LasVegasRuntimePhase.restore(new LasVegasSnapshot(
                1, 3, "RESOLVING", 10, 20, 0, "P1", "P1",
                players, List.of(30_000), casinos, List.of(), List.of(), List.of()
        ), new ZeroRandom());

        runtime.endGame();
        var results = runtime.buildView("P1", Map.of()).results();
        assertEquals("P2", results.get(0).playerId());
        assertEquals(1, results.get(0).rank());
        assertTrue(results.get(0).winner());
        assertEquals(2, results.get(1).rank());
        assertFalse(results.get(1).winner());
    }

    private static LasVegasRuntimePhase runtime(int count) {
        var start = new LasVegasStartPhase(players(count), new ZeroRandom());
        start.enter();
        var runtime = start.transit();
        runtime.drainPublicEvents();
        return runtime;
    }

    private static void playAllOnes(LasVegasRuntimePhase runtime, List<String> order) {
        for (String player : order) {
            long version = runtime.stateVersion();
            runtime.applyCommand(player, new LasVegasRuntimePhase.Command(version, "ROLL_DICE", null));
            runtime.applyCommand(player, new LasVegasRuntimePhase.Command(version + 1, "PLACE_DICE", 1));
        }
    }

    private static List<PlayerStartInfo> players(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new PlayerStartInfo("P" + index, "Player " + index, false, true))
                .toList();
    }

    private static LasVegasSnapshot resolvingSnapshot() {
        var playerStates = List.of(
                new LasVegasSnapshot.PlayerState("P1", "Player 1", 2, 0, false, List.of()),
                new LasVegasSnapshot.PlayerState("P2", "Player 2", 2, 0, false, List.of()),
                new LasVegasSnapshot.PlayerState("P3", "Player 3", 2, 0, false, List.of()),
                new LasVegasSnapshot.PlayerState("P4", "Player 4", 2, 0, false, List.of())
        );
        var casinos = new ArrayList<LasVegasSnapshot.CasinoState>();
        casinos.add(new LasVegasSnapshot.CasinoState(1, 100_000, 30_000, List.of(
                new LasVegasSnapshot.PlacementState("P1", 5, false),
                new LasVegasSnapshot.PlacementState("P2", 3, false),
                new LasVegasSnapshot.PlacementState("P3", 3, false),
                new LasVegasSnapshot.PlacementState("P4", 1, false)
        ), null));
        for (int number = 2; number <= 6; number++) {
            casinos.add(new LasVegasSnapshot.CasinoState(number, 40_000, 30_000, List.of(), null));
        }
        return new LasVegasSnapshot(
                1, 1, "RESOLVING", 4, 7, 0, "P1", "P1",
                playerStates,
                List.of(40_000, 50_000, 60_000),
                casinos,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
