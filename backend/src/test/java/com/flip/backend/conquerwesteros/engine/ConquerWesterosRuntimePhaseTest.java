package com.flip.backend.conquerwesteros.engine;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ResultView;
import com.flip.backend.conquerwesteros.entities.BattleLine;
import com.flip.backend.conquerwesteros.entities.DieFace;
import com.flip.backend.security.GameStateConflictException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConquerWesterosRuntimePhaseTest {
    @Test
    void campaignsShareAllFourteenTemplatesAndValidatedTotals() {
        var fiveKings = ConquerWesterosCatalog.campaign(Campaign.WAR_OF_FIVE_KINGS);
        var dance = ConquerWesterosCatalog.campaign(Campaign.DANCE_OF_THE_DRAGONS);

        assertEquals(14, fiveKings.strongholds().size());
        assertEquals(6, fiveKings.clanScores().size());
        assertEquals(29, fiveKings.strongholds().stream().mapToInt(card -> card.points()).sum());
        assertEquals(37, fiveKings.clanScores().values().stream().mapToInt(Integer::intValue).sum());
        for (String id : ids()) {
            assertEquals(fiveKings.stronghold(id).points(), dance.stronghold(id).points());
            assertEquals(fiveKings.stronghold(id).lines(), dance.stronghold(id).lines());
        }
    }

    @Test
    void battleLinesAcceptMilitaryOverageAndRequireExactSymbolMultisets() {
        var military = new BattleLine.Military("M", 5);
        assertTrue(military.matches(List.of(DieFace.MILITARY_3, DieFace.MILITARY_3)));
        assertFalse(military.matches(List.of(DieFace.MILITARY_3, DieFace.MILITARY_1)));
        assertFalse(military.matches(List.of(DieFace.MILITARY_3, DieFace.RAVEN, DieFace.MILITARY_2)));

        var symbols = new BattleLine.Symbols("S", List.of(DieFace.RAVEN, DieFace.KNIGHT));
        assertTrue(symbols.matches(List.of(DieFace.KNIGHT, DieFace.RAVEN)));
        assertFalse(symbols.matches(List.of(DieFace.KNIGHT, DieFace.RAVEN, DieFace.CROWN)));
        assertFalse(symbols.matches(List.of(DieFace.KNIGHT, DieFace.KNIGHT)));
    }

    @Test
    void everyRollIsManualAndKeepsStableDiceIdsAfterALoss() {
        var runtime = newGame(new SequenceRandom(0, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0));
        runtime.applyCommand("P1", command(0, "ROLL_DICE"));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), runtime.buildView("P1").currentRoll().stream().map(die -> die.dieId()).toList());

        runtime.applyCommand("P1", new ConquerWesterosRuntimePhase.Command(1, "LOSE_DIE", null, null, List.of(), 0));
        GameView waiting = runtime.buildView("P1");
        assertEquals("WAITING_FOR_ROLL", waiting.phase());
        assertTrue(waiting.currentRoll().isEmpty());
        assertEquals(List.of(0), waiting.attempt().lostDieIds());

        runtime.applyCommand("P1", command(2, "ROLL_DICE"));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), runtime.buildView("P1").currentRoll().stream().map(die -> die.dieId()).toList());
    }

    @Test
    void firstCompletedLineFreezesTargetAndSnapshotRestoresThePartialSiege() {
        var runtime = newGame(new SequenceRandom(0, 2, 1, 0, 0, 0, 0, 0));
        runtime.applyCommand("P1", command(0, "ROLL_DICE"));
        runtime.applyCommand("P1", complete(1, "T05", "L1", 0, 1));

        GameView partial = runtime.buildView("P1");
        assertEquals("WAITING_FOR_ROLL", partial.phase());
        assertEquals("T05", partial.attempt().targetId());
        assertEquals(List.of("L1"), partial.attempt().completedLineIds());
        assertEquals(runtime.snapshot(), ConquerWesterosRuntimePhase.restore(runtime.snapshot(), new SequenceRandom()).snapshot());

        runtime.applyCommand("P1", command(2, "ROLL_DICE"));
        assertThrows(IllegalArgumentException.class, () ->
                runtime.applyCommand("P1", complete(3, "T01", "L1", 2, 3, 4, 5, 6)));
        assertThrows(GameStateConflictException.class, () -> runtime.applyCommand("P1", command(2, "ROLL_DICE")));
    }

    @Test
    void stealingAddsAnIndependentCrownAndPrintedCrownCannotCompleteBoth() {
        var start = snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                without("T10"),
                List.of(playerState("P1", List.of(), Map.of()), playerState("P2", List.of("T10"), Map.of())),
                attempt(null, null, false, Map.of(), List.of()),
                List.of(roll(0, DieFace.MILITARY_3), roll(1, DieFace.MILITARY_1)),
                "P2",
                0
        );
        var runtime = ConquerWesterosRuntimePhase.restore(start, new SequenceRandom());
        runtime.applyCommand("P1", complete(0, "T10", "L1", 0, 1));
        var lineIds = runtime.buildView("P1").attempt().requiredLines().stream().map(line -> line.id()).toList();
        assertEquals(List.of("L1", "L2", "L3", "STEAL_CROWN"), lineIds);

        var printedCrown = snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                without("T10"),
                List.of(playerState("P1", List.of(), Map.of()), playerState("P2", List.of("T10"), Map.of())),
                attempt("T10", "P2", true, Map.of("L1", List.of(0, 1), "L2", List.of(2, 3)), List.of()),
                List.of(roll(4, DieFace.CROWN), roll(5, DieFace.MILITARY_1), roll(6, DieFace.MILITARY_1)),
                "P2",
                0
        );
        runtime = ConquerWesterosRuntimePhase.restore(printedCrown, new SequenceRandom());
        runtime.applyCommand("P1", complete(0, "T10", "L3", 4));
        GameView afterPrintedCrown = runtime.buildView("P1");
        assertEquals("WAITING_FOR_ROLL", afterPrintedCrown.phase());
        assertTrue(afterPrintedCrown.attempt().completedLineIds().contains("L3"));
        assertFalse(afterPrintedCrown.attempt().completedLineIds().contains("STEAL_CROWN"));
        assertEquals("P2", stronghold(afterPrintedCrown, "T10").ownerId());
    }

    @Test
    void failedSiegeClearsEveryDieAndLineBeforeAdvancing() {
        var saved = snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                ids(),
                basicPlayers(),
                attempt("T05", null, false, Map.of("L1", List.of(0, 1)), List.of(2, 3, 4, 5)),
                List.of(roll(6, DieFace.CROWN)),
                null,
                4
        );
        var runtime = ConquerWesterosRuntimePhase.restore(saved, new SequenceRandom());
        runtime.applyCommand("P1", new ConquerWesterosRuntimePhase.Command(4, "LOSE_DIE", null, null, List.of(), 6));

        GameView next = runtime.buildView("P1");
        assertEquals("P2", next.currentPlayerId());
        assertEquals(1, next.turnCount());
        assertEquals("WAITING_FOR_ROLL", next.phase());
        assertEquals(null, next.attempt().targetId());
        assertTrue(next.attempt().completedLineIds().isEmpty());
        assertTrue(next.attempt().lostDieIds().isEmpty());
    }

    @Test
    void kingsLandingTransfersTheThroneBeforeTheTurnAdvances() {
        var runtime = ConquerWesterosRuntimePhase.restore(snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                ids(),
                basicPlayers(),
                attempt("T10", null, false, Map.of("L1", List.of(0, 1), "L2", List.of(2, 3)), List.of()),
                List.of(roll(4, DieFace.CROWN), roll(5, DieFace.MILITARY_1), roll(6, DieFace.MILITARY_1)),
                null,
                0
        ), new SequenceRandom());

        runtime.applyCommand("P1", complete(0, "T10", "L3", 4));
        GameView view = runtime.buildView("P1");
        assertEquals("P1", view.ironThroneHolderId());
        assertEquals("P1", stronghold(view, "T10").ownerId());
        assertEquals("P2", view.currentPlayerId());
        assertTrue(view.events().stream().map(event -> event.type()).toList().indexOf("STRONGHOLD_CAPTURED")
                < view.events().stream().map(event -> event.type()).toList().indexOf("IRON_THRONE_TRANSFERRED"));
    }

    @Test
    void stealingAnyFaceUpCardFromTheThroneHolderTransfersTheToken() {
        var runtime = ConquerWesterosRuntimePhase.restore(snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                without("T03"),
                List.of(playerState("P1", List.of(), Map.of()), playerState("P2", List.of("T03"), Map.of())),
                attempt("T03", "P2", true, Map.of("L1", List.of(0, 1)), List.of()),
                List.of(roll(2, DieFace.CROWN), roll(3, DieFace.MILITARY_1), roll(4, DieFace.MILITARY_1), roll(5, DieFace.MILITARY_1), roll(6, DieFace.MILITARY_1)),
                "P2",
                0
        ), new SequenceRandom());

        runtime.applyCommand("P1", complete(0, "T03", "STEAL_CROWN", 2));
        GameView view = runtime.buildView("P1");
        assertEquals("P1", view.ironThroneHolderId());
        assertEquals("P1", stronghold(view, "T03").ownerId());
    }

    @Test
    void aSingleCardClanLocksImmediatelyAndCannotBeTargeted() {
        var runtime = ConquerWesterosRuntimePhase.restore(snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                ids(),
                basicPlayers(),
                attempt("T13", null, false, Map.of("L1", List.of(0, 1), "L2", List.of(2, 3)), List.of()),
                List.of(roll(4, DieFace.KNIGHT), roll(5, DieFace.MILITARY_1), roll(6, DieFace.MILITARY_1)),
                null,
                0
        ), new SequenceRandom());
        runtime.applyCommand("P1", complete(0, "T13", "L3", 4));

        GameView view = runtime.buildView("P1");
        assertTrue(stronghold(view, "T13").locked());
        assertEquals(List.of("Arryn"), view.players().get(0).completedClans().stream().map(clan -> clan.name()).toList());

        var lockedTurn = ConquerWesterosRuntimePhase.restore(snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION,
                without("T13"),
                List.of(playerState("P1", List.of(), Map.of()), playerState("P2", List.of(), Map.of("Arryn", List.of("T13")))),
                attempt(null, null, false, Map.of(), List.of()),
                List.of(roll(0, DieFace.MILITARY_3), roll(1, DieFace.MILITARY_2)),
                null,
                0
        ), new SequenceRandom());
        assertFalse(lockedTurn.buildView("P1").legalActions().legalTargetIds().contains("T13"));
        assertThrows(IllegalArgumentException.class, () -> lockedTurn.applyCommand("P1", complete(0, "T13", "L1", 0, 1)));
    }

    @Test
    void snapshotRoundTripsRolledAndWaitingToRerollIntermediateStates() {
        var runtime = newGame(new SequenceRandom(0, 3, 4, 5, 0, 1, 2, 3));
        runtime.applyCommand("P1", command(0, "ROLL_DICE"));
        var rolled = runtime.snapshot();
        assertEquals(rolled, ConquerWesterosRuntimePhase.restore(rolled, new SequenceRandom()).snapshot());

        runtime.applyCommand("P1", new ConquerWesterosRuntimePhase.Command(1, "LOSE_DIE", null, null, List.of(), 0));
        var waiting = runtime.snapshot();
        assertEquals("WAITING_FOR_ROLL", waiting.phase());
        assertEquals(waiting, ConquerWesterosRuntimePhase.restore(waiting, new SequenceRandom()).snapshot());
    }

    @Test
    void finalRankingUsesThroneThenStrongholdsThenClansAndSharesFullyEqualRanks() {
        Map<String, ResultView> throne = resultsByPlayer(endGame(
                playerState("P1", List.of("T14"), Map.of()),
                playerState("P2", List.of("T11", "T05"), Map.of()),
                "P1"));
        assertTrue(throne.get("P1").rank() < throne.get("P2").rank());

        Map<String, ResultView> strongholds = resultsByPlayer(endGame(
                playerState("P1", List.of("T01", "T03"), Map.of()),
                playerState("P2", List.of("T05"), Map.of()),
                "P3"));
        assertTrue(strongholds.get("P1").rank() < strongholds.get("P2").rank());

        Map<String, ResultView> clans = resultsByPlayer(endGame(
                playerState("P1", List.of(), Map.of("Arryn", List.of("T13"))),
                playerState("P2", List.of("T12"), Map.of()),
                "P3"));
        assertTrue(clans.get("P1").rank() < clans.get("P2").rank());

        Map<String, ResultView> shared = resultsByPlayer(endGame(
                playerState("P1", List.of("T11"), Map.of()),
                playerState("P2", List.of("T12"), Map.of()),
                "P3"));
        assertEquals(shared.get("P1").rank(), shared.get("P2").rank());
    }

    private static ConquerWesterosRuntimePhase newGame(Random random) {
        return ConquerWesterosRuntimePhase.newGame(List.of(
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("P2", "Player 2", false, true)
        ), Campaign.WAR_OF_FIVE_KINGS, random);
    }

    private static ConquerWesterosRuntimePhase.Command command(long version, String type) {
        return new ConquerWesterosRuntimePhase.Command(version, type, null, null, List.of(), null);
    }

    private static ConquerWesterosRuntimePhase.Command complete(long version, String target, String line, Integer... dice) {
        return new ConquerWesterosRuntimePhase.Command(version, "COMPLETE_LINE", target, line, List.of(dice), null);
    }

    private static ConquerWesterosSnapshot.PlayerState playerState(
            String id,
            List<String> faceUp,
            Map<String, List<String>> completed
    ) {
        return new ConquerWesterosSnapshot.PlayerState(id, "Player " + id.substring(1), faceUp, completed);
    }

    private static List<ConquerWesterosSnapshot.PlayerState> basicPlayers() {
        return List.of(playerState("P1", List.of(), Map.of()), playerState("P2", List.of(), Map.of()));
    }

    private static ConquerWesterosSnapshot.AttemptState attempt(
            String target,
            String owner,
            boolean stealing,
            Map<String, List<Integer>> lines,
            List<Integer> lost
    ) {
        return new ConquerWesterosSnapshot.AttemptState(target, owner, stealing, lines, lost);
    }

    private static ConquerWesterosSnapshot.RollState roll(int id, DieFace face) {
        return new ConquerWesterosSnapshot.RollState(id, face.name());
    }

    private static ConquerWesterosSnapshot snapshot(
            Campaign campaign,
            ConquerWesterosRuntimePhase.State state,
            List<String> central,
            List<ConquerWesterosSnapshot.PlayerState> players,
            ConquerWesterosSnapshot.AttemptState attempt,
            List<ConquerWesterosSnapshot.RollState> roll,
            String throne,
            long version
    ) {
        return new ConquerWesterosSnapshot(
                1, campaign.name(), state.name(), 0, version, 0, "P1", throne, central,
                players, attempt, roll, List.of(), List.of());
    }

    private static List<String> ids() {
        return ConquerWesterosCatalog.campaign(Campaign.WAR_OF_FIVE_KINGS).strongholds().stream().map(card -> card.id()).toList();
    }

    private static List<String> without(String id) {
        return ids().stream().filter(candidate -> !candidate.equals(id)).toList();
    }

    private static com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.StrongholdView stronghold(GameView view, String id) {
        return view.strongholds().stream().filter(card -> card.id().equals(id)).findFirst().orElseThrow();
    }

    private static List<ResultView> endGame(
            ConquerWesterosSnapshot.PlayerState p1,
            ConquerWesterosSnapshot.PlayerState p2,
            String throne
    ) {
        Set<String> used = new LinkedHashSet<>();
        used.addAll(p1.faceUpStrongholds());
        p1.completedClans().values().forEach(used::addAll);
        used.addAll(p2.faceUpStrongholds());
        p2.completedClans().values().forEach(used::addAll);
        var p3Cards = ids().stream().filter(id -> !used.contains(id)).toList();
        var runtime = ConquerWesterosRuntimePhase.restore(snapshot(
                Campaign.WAR_OF_FIVE_KINGS,
                ConquerWesterosRuntimePhase.State.RESOLVING,
                List.of(),
                List.of(p1, p2, playerState("P3", p3Cards, Map.of())),
                attempt(null, null, false, Map.of(), List.of()),
                List.of(), throne, 0
        ), new SequenceRandom());
        assertTrue(runtime.canEndGame());
        runtime.endGame();
        assertEquals("FINISHED", runtime.buildView("P1").phase());
        return runtime.buildView("P1").results();
    }

    private static Map<String, ResultView> resultsByPlayer(List<ResultView> results) {
        var byPlayer = new LinkedHashMap<String, ResultView>();
        results.forEach(result -> byPlayer.put(result.playerId(), result));
        assertNotNull(byPlayer.get("P1"));
        assertNotNull(byPlayer.get("P2"));
        return byPlayer;
    }

    private static final class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        private SequenceRandom(int... values) { this.values = values; }

        @Override public int nextInt(int bound) {
            int value = index < values.length ? values[index++] : 0;
            return Math.floorMod(value, bound);
        }
    }
}
