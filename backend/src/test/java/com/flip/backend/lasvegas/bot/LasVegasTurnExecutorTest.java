package com.flip.backend.lasvegas.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.LasVegasWsService;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.lasvegas.LasVegasPresentationService;
import com.flip.backend.lasvegas.engine.LasVegasGameRegistry;
import com.flip.backend.lasvegas.engine.LasVegasSnapshotCodec;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
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

class LasVegasTurnExecutorTest {
    @Test
    void botRollAndChoiceCommitAsSeparateGuardedVersions() {
        Fixture fixture = fixture(List.of(
                player("BOT1", true), player("P1", false), player("BOT2", true)
        ));
        LasVegasBotTicket rollTicket = fixture.runtime.botTicket(fixture.entity.getId());

        var rolled = fixture.executor.executeBot(rollTicket);

        assertTrue(rolled.applied());
        assertEquals(1, fixture.runtime.stateVersion());
        assertEquals(LasVegasRuntimePhase.State.WAITING_FOR_CHOICE, fixture.runtime.state());
        assertNotNull(rolled.nextBotTicket());
        assertEquals(1, rolled.publicEvents().size());
        assertEquals("ROLL_DICE", rolled.publicEvents().get(0).type());

        var placed = fixture.executor.executeBot(rolled.nextBotTicket());

        assertTrue(placed.applied());
        assertEquals(2, fixture.runtime.stateVersion());
        assertEquals("P1", fixture.runtime.currentPlayerId());
        assertNull(placed.nextBotTicket());
        assertEquals("PLACE_DICE", placed.publicEvents().get(0).type());
        verify(fixture.ws).broadcastEvents(fixture.entity.getId(), rolled.publicEvents());
        verify(fixture.ws).broadcastEvents(fixture.entity.getId(), placed.publicEvents());

        var stale = fixture.executor.executeBot(rollTicket);
        assertFalse(stale.applied());
        assertEquals(2, fixture.runtime.stateVersion());
    }

    @Test
    void consecutiveBotsProduceTheNextBotsTicket() {
        Fixture fixture = fixture(List.of(
                player("BOT1", true), player("BOT2", true), player("P1", false)
        ));

        var firstRoll = fixture.executor.executeBot(fixture.runtime.botTicket(fixture.entity.getId()));
        var firstPlace = fixture.executor.executeBot(firstRoll.nextBotTicket());

        assertNotNull(firstPlace.nextBotTicket());
        assertEquals("BOT2", firstPlace.nextBotTicket().botId());
        assertEquals(LasVegasRuntimePhase.State.WAITING_FOR_ROLL, firstPlace.nextBotTicket().expectedPhase());
    }

    @Test
    void humanPlacementReturnsATicketWhenControlPassesToABot() {
        Fixture fixture = fixture(List.of(
                player("P1", false), player("BOT1", true), player("BOT2", true)
        ));

        fixture.executor.executeHuman(fixture.entity.getId(), "P1",
                new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        var placed = fixture.executor.executeHuman(fixture.entity.getId(), "P1",
                new LasVegasRuntimePhase.Command(1, "PLACE_DICE", 1));

        assertNotNull(placed.nextBotTicket());
        assertEquals("BOT1", placed.nextBotTicket().botId());
    }

    @Test
    void mixedTableCanRunThroughTheFinalBotActionAndEndTheSession() {
        Fixture fixture = fixture(List.of(
                player("P1", false), player("BOT1", true), player("BOT2", true)
        ));
        int actions = 0;
        while (fixture.runtime.state() != LasVegasRuntimePhase.State.FINISHED && actions++ < 100) {
            if (fixture.runtime.currentPlayerIsBot()) {
                fixture.executor.executeBot(fixture.runtime.botTicket(fixture.entity.getId()));
            } else if (fixture.runtime.state() == LasVegasRuntimePhase.State.WAITING_FOR_ROLL) {
                fixture.executor.executeHuman(fixture.entity.getId(), fixture.runtime.currentPlayerId(),
                        new LasVegasRuntimePhase.Command(fixture.runtime.stateVersion(), "ROLL_DICE", null));
            } else {
                fixture.executor.executeHuman(fixture.entity.getId(), fixture.runtime.currentPlayerId(),
                        new LasVegasRuntimePhase.Command(fixture.runtime.stateVersion(), "PLACE_DICE", fixture.runtime.lowestLegalFace()));
            }
        }

        assertEquals(LasVegasRuntimePhase.State.FINISHED, fixture.runtime.state());
        assertTrue(actions < 100);
        assertEquals("ENDED", fixture.entity.getState());
        assertEquals("ENDED", fixture.session.getState());
        assertEquals(3, fixture.runtime.buildView("P1", java.util.Map.of()).results().size());
    }

    private static Fixture fixture(List<PlayerStartInfo> players) {
        var runtime = LasVegasRuntimePhase.newGame(players, new ZeroRandom());
        runtime.drainPublicEvents();
        String gameId = "game-1";
        var entity = GameEntity.builder()
                .id(gameId)
                .sessionId("session-1")
                .roundIndex(1)
                .gameType("LASVEGAS")
                .state("RUNNING")
                .createdAt(Instant.now())
                .build();
        var sessions = mock(SessionRepository.class);
        var session = SessionEntity.builder().id("session-1").state("RUNNING").build();
        var games = mock(GameRepository.class);
        var registry = mock(LasVegasGameRegistry.class);
        var ws = mock(LasVegasWsService.class);
        var codec = new LasVegasSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        when(games.findByIdForUpdate(gameId)).thenReturn(Optional.of(entity));
        when(registry.getForUpdate(entity)).thenReturn(runtime);
        when(sessions.findByIdForUpdate("session-1")).thenReturn(Optional.of(session));
        var executor = new LasVegasTurnExecutor(
                sessions, games, registry, codec, new LasVegasPresentationService(), ws,
                new StandardLasVegasBotStrategy(), new SimpleMeterRegistry()
        );
        return new Fixture(executor, runtime, entity, session, ws);
    }

    private static PlayerStartInfo player(String id, boolean bot) {
        return new PlayerStartInfo(id, id, bot, true);
    }

    private record Fixture(
            LasVegasTurnExecutor executor,
            LasVegasRuntimePhase runtime,
            GameEntity entity,
            SessionEntity session,
            LasVegasWsService ws
    ) {}

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
