package com.flip.backend.service.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.LasVegasWsService;
import com.flip.backend.api.dto.LobbyDtos.PlayerSpec;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.lasvegas.LasVegasPresentationService;
import com.flip.backend.lasvegas.engine.LasVegasGameRegistry;
import com.flip.backend.lasvegas.engine.LasVegasSnapshotCodec;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LasVegasGameServiceTest {
    @Test
    void exposesFixedCapabilitiesAndRejectsSeriesBotsAndInvalidCounts() {
        Fixture fixture = fixture();
        assertEquals(new GameCapabilities(3, 10, false, false, 3), fixture.service.capabilities());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(2, 1, false)));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(3, 2, false)));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(3, 1, true)));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(11, 1, false)));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startNext("session", request(3, 1, false)));
    }

    @Test
    void startsOnePersistedAggregateWithRoundIndexOne() {
        Fixture fixture = fixture();

        var response = fixture.service.startFirst("session", request(3, 1, false));

        assertEquals(1, response.roundIndex());
        assertEquals("P1", response.myPlayerId());
        assertEquals(List.of("P1", "P2", "P3"), response.players().stream().map(player -> player.playerId()).toList());
        GameEntity persisted = fixture.entities.get(response.gameId());
        assertNotNull(persisted.getStateJson());
        assertTrue(persisted.getStateJson().contains("\"schemaVersion\":1"));
        assertEquals("RUNNING", fixture.session.getState());
    }

    private static StartGameRequest request(int count, int rounds, boolean bot) {
        var players = new ArrayList<PlayerSpec>();
        for (int index = 1; index <= count; index++) {
            players.add(new PlayerSpec("Player " + index, bot && index == count, true));
        }
        return new StartGameRequest(rounds, players);
    }

    private static Fixture fixture() {
        var sessions = mock(SessionRepository.class);
        var games = mock(GameRepository.class);
        var ws = mock(LasVegasWsService.class);
        var codec = new LasVegasSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var registry = mock(LasVegasGameRegistry.class);
        var presentation = new LasVegasPresentationService();
        var session = SessionEntity.builder()
                .id("session")
                .gameType("LASVEGAS")
                .state("LOBBY")
                .maxPlayers(10)
                .build();
        var entities = new LinkedHashMap<String, GameEntity>();
        when(sessions.findByIdForUpdate("session")).thenReturn(Optional.of(session));
        when(games.findTopBySessionIdOrderByRoundIndexDesc("session")).thenReturn(Optional.empty());
        when(games.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity entity = invocation.getArgument(0);
            entities.put(entity.getId(), entity);
            return entity;
        });
        when(games.findById(any())).thenAnswer(invocation -> Optional.ofNullable(entities.get(invocation.<String>getArgument(0))));

        var service = new LasVegasGameService(
                sessions, games, registry, codec, presentation, ws, ZeroRandom::new
        );
        return new Fixture(service, session, entities);
    }

    private record Fixture(LasVegasGameService service, SessionEntity session, LinkedHashMap<String, GameEntity> entities) {}

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
