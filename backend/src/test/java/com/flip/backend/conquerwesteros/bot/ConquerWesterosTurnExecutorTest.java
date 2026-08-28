package com.flip.backend.conquerwesteros.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.ConquerWesterosWsService;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.ConquerWesterosTurnExecutor;
import com.flip.backend.conquerwesteros.engine.Campaign;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosCatalog;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosGameRegistry;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshot;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConquerWesterosTurnExecutorTest {
    @Test
    void botRollAndDecisionCommitAsSeparateGuardedVersions() {
        Fixture fixture = fixture(List.of(player("BOT1", true), player("P1", false)));
        ConquerWesterosBotTicket rollTicket = fixture.runtime.botTicket(fixture.entity.getId());

        var rolled = fixture.executor.executeBot(rollTicket);

        assertTrue(rolled.applied());
        assertEquals(1, fixture.runtime.stateVersion());
        assertEquals(ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION, fixture.runtime.state());
        assertNotNull(rolled.nextBotTicket());
        assertEquals(List.of("ROLL_DICE"), rolled.publicEvents().stream().map(event -> event.type()).toList());

        var decided = fixture.executor.executeBot(rolled.nextBotTicket());

        assertTrue(decided.applied());
        assertEquals(2, fixture.runtime.stateVersion());
        assertEquals("P1", fixture.runtime.currentPlayerId());
        assertNull(decided.nextBotTicket());
        assertTrue(decided.publicEvents().stream().anyMatch(event -> event.type().equals("STRONGHOLD_CAPTURED")));
        verify(fixture.ws).broadcastEvents(fixture.entity.getId(), rolled.publicEvents());
        verify(fixture.ws).broadcastEvents(fixture.entity.getId(), decided.publicEvents());

        var stale = fixture.executor.executeBot(rollTicket);
        assertFalse(stale.applied());
        assertEquals(2, fixture.runtime.stateVersion());
    }

    @Test
    void consecutiveBotsProduceTheNextBotsTicket() {
        Fixture fixture = fixture(List.of(player("BOT1", true), player("BOT2", true), player("P1", false)));

        var firstRoll = fixture.executor.executeBot(fixture.runtime.botTicket(fixture.entity.getId()));
        var firstDecision = fixture.executor.executeBot(firstRoll.nextBotTicket());

        assertNotNull(firstDecision.nextBotTicket());
        assertEquals("BOT2", firstDecision.nextBotTicket().botId());
        assertEquals(ConquerWesterosRuntimePhase.State.WAITING_FOR_ROLL,
                firstDecision.nextBotTicket().expectedPhase());
    }

    @Test
    void humanCaptureReturnsATicketWhenControlPassesToABot() {
        Fixture fixture = fixture(List.of(player("P1", false), player("BOT1", true)));

        fixture.executor.execute(fixture.entity.getId(), "P1", command(0, "ROLL_DICE"));
        var captured = fixture.executor.execute(fixture.entity.getId(), "P1",
                new ConquerWesterosRuntimePhase.Command(1, "COMPLETE_LINE", "T01", "L1",
                        List.of(0, 1, 2, 3, 4), null));

        assertNotNull(captured.nextBotTicket());
        assertEquals("BOT1", captured.nextBotTicket().botId());
    }

    @Test
    void finalBotCaptureEndsBothGameAndSession() {
        var players = List.of(
                new ConquerWesterosSnapshot.PlayerState("BOT1", "Bot 1", true, List.of(), Map.of()),
                new ConquerWesterosSnapshot.PlayerState("P1", "Player 1", false,
                        ids().stream().filter(id -> !id.equals("T01")).toList(), Map.of())
        );
        var runtime = ConquerWesterosRuntimePhase.restore(new ConquerWesterosSnapshot(
                2, Campaign.WAR_OF_FIVE_KINGS.name(), "WAITING_FOR_ROLL", 0, 0, 0,
                "BOT1", null, List.of("T01"), players,
                new ConquerWesterosSnapshot.AttemptState(null, null, false, Map.of(), List.of()),
                List.of(), List.of(), List.of()), new ZeroRandom());
        Fixture fixture = fixture(runtime);

        fixture.executor.executeBot(runtime.botTicket(fixture.entity.getId()));
        fixture.executor.executeBot(runtime.botTicket(fixture.entity.getId()));

        assertEquals(ConquerWesterosRuntimePhase.State.FINISHED, runtime.state());
        assertEquals("ENDED", fixture.entity.getState());
        assertEquals("ENDED", fixture.session.getState());
        assertEquals(2, runtime.buildView("P1").results().size());
    }

    @Test
    void illegalStrategyDecisionFallsBackToALegalDeterministicAction() {
        var runtime = ConquerWesterosRuntimePhase.newGame(
                List.of(player("BOT1", true), player("P1", false)),
                Campaign.WAR_OF_FIVE_KINGS, new ZeroRandom());
        ConquerWesterosBotStrategy illegal = ignored ->
                ConquerWesterosBotStrategy.Decision.completeLine("missing", "missing", List.of(99));
        Fixture fixture = fixture(runtime, illegal);

        fixture.executor.executeBot(runtime.botTicket(fixture.entity.getId()));
        var outcome = fixture.executor.executeBot(runtime.botTicket(fixture.entity.getId()));

        assertTrue(outcome.applied());
        assertEquals(2, runtime.stateVersion());
        assertEquals("T09", runtime.buildView("P1").attempt().targetId());
    }

    @Test
    void mixedTableRunsThroughACompleteCampaignWithStandardBots() {
        var runtime = ConquerWesterosRuntimePhase.newGame(
                List.of(player("P1", false), player("BOT1", true)),
                Campaign.WAR_OF_FIVE_KINGS, new Random(20260826));
        Fixture fixture = fixture(runtime, new StandardConquerWesterosBotStrategy());
        int actions = 0;
        while (runtime.state() != ConquerWesterosRuntimePhase.State.FINISHED && actions++ < 3000) {
            if (runtime.currentPlayerIsBot()) {
                fixture.executor.executeBot(runtime.botTicket(fixture.entity.getId()));
            } else if (runtime.state() == ConquerWesterosRuntimePhase.State.WAITING_FOR_ROLL) {
                fixture.executor.execute(fixture.entity.getId(), "P1",
                        command(runtime.stateVersion(), "ROLL_DICE"));
            } else {
                fixture.executor.execute(fixture.entity.getId(), "P1", humanDecision(runtime.buildView("P1")));
            }
        }

        assertEquals(ConquerWesterosRuntimePhase.State.FINISHED, runtime.state());
        assertTrue(actions < 3000);
        assertEquals("ENDED", fixture.entity.getState());
        assertEquals("ENDED", fixture.session.getState());
    }

    private static Fixture fixture(List<PlayerStartInfo> players) {
        return fixture(ConquerWesterosRuntimePhase.newGame(
                players, Campaign.WAR_OF_FIVE_KINGS, new ZeroRandom()));
    }

    private static Fixture fixture(ConquerWesterosRuntimePhase runtime) {
        ConquerWesterosBotStrategy captureT01 = ignored ->
                ConquerWesterosBotStrategy.Decision.completeLine("T01", "L1", List.of(0, 1, 2, 3, 4));
        return fixture(runtime, captureT01);
    }

    private static Fixture fixture(
            ConquerWesterosRuntimePhase runtime,
            ConquerWesterosBotStrategy strategy
    ) {
        runtime.drainPublicEvents();
        String gameId = "game-1";
        var entity = GameEntity.builder().id(gameId).sessionId("session-1").roundIndex(1)
                .gameType("CONQUERWESTEROS").state("RUNNING").createdAt(Instant.now()).build();
        var sessions = mock(SessionRepository.class);
        var session = SessionEntity.builder().id("session-1").state("RUNNING").build();
        var games = mock(GameRepository.class);
        var registry = mock(ConquerWesterosGameRegistry.class);
        var ws = mock(ConquerWesterosWsService.class);
        var codec = new ConquerWesterosSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        when(games.findByIdForUpdate(gameId)).thenReturn(Optional.of(entity));
        when(registry.getForUpdate(entity)).thenReturn(runtime);
        when(sessions.findByIdForUpdate("session-1")).thenReturn(Optional.of(session));
        var executor = new ConquerWesterosTurnExecutor(sessions, games, registry, codec, ws,
                strategy, new SimpleMeterRegistry());
        return new Fixture(executor, runtime, entity, session, ws);
    }

    private static ConquerWesterosRuntimePhase.Command command(long version, String type) {
        return new ConquerWesterosRuntimePhase.Command(version, type, null, null, List.of(), null);
    }

    private static ConquerWesterosRuntimePhase.Command humanDecision(ConquerWesterosView.GameView view) {
        var targets = view.strongholds().stream()
                .filter(card -> view.legalActions().legalTargetIds().contains(card.id()))
                .sorted(java.util.Comparator.comparing((ConquerWesterosView.StrongholdView card) -> !card.central())
                        .thenComparing(ConquerWesterosView.StrongholdView::id))
                .toList();
        for (var target : targets) {
            List<ConquerWesterosView.LineView> lines = view.attempt().targetId() == null
                    ? target.lines() : view.attempt().requiredLines();
            for (var line : lines) {
                if (line.completed()) continue;
                List<Integer> dice = matchingDice(line, view.currentRoll());
                if (!dice.isEmpty()) {
                    return new ConquerWesterosRuntimePhase.Command(view.stateVersion(), "COMPLETE_LINE",
                            target.id(), line.id(), dice, null);
                }
            }
        }
        int dieId = view.currentRoll().stream().mapToInt(die -> die.dieId()).min().orElseThrow();
        return new ConquerWesterosRuntimePhase.Command(
                view.stateVersion(), "LOSE_DIE", null, null, List.of(), dieId);
    }

    private static List<Integer> matchingDice(
            ConquerWesterosView.LineView line,
            List<ConquerWesterosView.DieView> roll
    ) {
        if ("MILITARY".equals(line.type())) {
            var candidates = roll.stream().filter(die -> die.militaryStrength() > 0)
                    .sorted(java.util.Comparator.comparingInt(ConquerWesterosView.DieView::militaryStrength).reversed()
                            .thenComparingInt(ConquerWesterosView.DieView::dieId))
                    .toList();
            int total = 0;
            var ids = new java.util.ArrayList<Integer>();
            for (var die : candidates) {
                ids.add(die.dieId());
                total += die.militaryStrength();
                if (total >= line.threshold()) return ids.stream().sorted().toList();
            }
            return List.of();
        }
        var used = new java.util.HashSet<Integer>();
        var ids = new java.util.ArrayList<Integer>();
        for (String symbol : line.symbols()) {
            var die = roll.stream().filter(candidate -> !used.contains(candidate.dieId()) && candidate.face().equals(symbol))
                    .min(java.util.Comparator.comparingInt(ConquerWesterosView.DieView::dieId)).orElse(null);
            if (die == null) return List.of();
            used.add(die.dieId());
            ids.add(die.dieId());
        }
        return ids.stream().sorted().toList();
    }

    private static PlayerStartInfo player(String id, boolean bot) {
        return new PlayerStartInfo(id, id, bot, true);
    }

    private static List<String> ids() {
        return ConquerWesterosCatalog.campaign(Campaign.WAR_OF_FIVE_KINGS).strongholds().stream()
                .map(card -> card.id()).toList();
    }

    private record Fixture(
            ConquerWesterosTurnExecutor executor,
            ConquerWesterosRuntimePhase runtime,
            GameEntity entity,
            SessionEntity session,
            ConquerWesterosWsService ws
    ) {}

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
