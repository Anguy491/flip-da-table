package com.flip.backend.lasvegas.engine;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.engine.phase.LasVegasStartPhase;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GamePlayerEntity;
import com.flip.backend.persistence.GamePlayerRepository;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionMemberEntity;
import com.flip.backend.persistence.SessionMemberRepository;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.security.GamePlayerRegistry;
import com.flip.backend.security.GameStateConflictException;
import com.flip.backend.service.game.LasVegasGameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.Authentication;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.jwt.secret=01234567890123456789012345678901",
        "app.auth.password-reset.enabled=false",
        "app.auth.google.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class LasVegasRestartIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired UserRepository users;
    @Autowired SessionRepository sessions;
    @Autowired SessionMemberRepository members;
    @Autowired GameRepository games;
    @Autowired GamePlayerRepository gamePlayers;
    @Autowired LasVegasSnapshotCodec codec;
    @Autowired LasVegasGameService service;

    @Test
    void restoresDeckPlayersIdentityAndCurrentChoiceFromPostgresAfterCacheLoss() {
        var userEntities = new ArrayList<UserEntity>();
        for (int index = 1; index <= 3; index++) {
            userEntities.add(users.save(UserEntity.builder()
                    .email("vegas" + index + "@example.com")
                    .passwordHash("unused-test-hash")
                    .nickname("Player " + index)
                    .roles("USER")
                    .createdAt(Instant.now())
                    .build()));
        }
        String sessionId = "11111111-1111-1111-1111-111111111111";
        sessions.save(SessionEntity.builder()
                .id(sessionId)
                .ownerId(userEntities.get(0).getId())
                .gameType("LASVEGAS")
                .maxPlayers(10)
                .state("RUNNING")
                .createdAt(Instant.now())
                .build());
        for (int index = 0; index < userEntities.size(); index++) {
            members.save(SessionMemberEntity.builder()
                    .sessionId(sessionId)
                    .userId(userEntities.get(index).getId())
                    .nickname("Player " + (index + 1))
                    .joinedAt(Instant.now().plusMillis(index))
                    .build());
        }

        var start = new LasVegasStartPhase(List.of(
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("P2", "Player 2", false, true),
                new PlayerStartInfo("P3", "Player 3", false, true)
        ), new ZeroRandom());
        start.enter();
        LasVegasRuntimePhase runtime = start.transit();
        runtime.drainPublicEvents();
        runtime.applyCommand("P1", new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
        LasVegasSnapshot expected = runtime.snapshot();

        String gameId = sessionId + ":LASVEGAS:r1";
        games.save(GameEntity.builder()
                .id(gameId)
                .sessionId(sessionId)
                .roundIndex(1)
                .gameType("LASVEGAS")
                .state("RUNNING")
                .stateJson(codec.encode(runtime))
                .createdAt(Instant.now())
                .build());
        for (int index = 0; index < userEntities.size(); index++) {
            gamePlayers.save(GamePlayerEntity.builder()
                    .gameId(gameId)
                    .userId(userEntities.get(index).getId())
                    .playerId("P" + (index + 1))
                    .seatIndex(index)
                    .build());
        }

        // New instances simulate an empty process-local runtime and identity cache.
        var restartedRegistry = new LasVegasGameRegistry(games, codec);
        LasVegasRuntimePhase restored = restartedRegistry.get(gameId);
        assertNotNull(restored);
        assertEquals(expected, restored.snapshot());
        assertEquals("WAITING_FOR_CHOICE", restored.buildView("P1", java.util.Map.of()).phase());
        assertEquals(8, restored.buildView("P1", java.util.Map.of()).currentRoll().size());

        var access = new GameAccessService(users, sessions, members, games, new GamePlayerRegistry(), gamePlayers);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("vegas1@example.com");
        assertEquals("P1", access.requirePlayer(authentication, gameId));
        assertEquals(3, gamePlayers.findByGameIdOrderBySeatIndexAsc(gameId).size());
    }

    @Test
    void rowLockAllowsOnlyOneCommandAtTheSameExpectedVersion() throws Exception {
        String sessionId = "22222222-2222-2222-2222-222222222222";
        sessions.save(SessionEntity.builder()
                .id(sessionId)
                .ownerId(1L)
                .gameType("LASVEGAS")
                .maxPlayers(10)
                .state("RUNNING")
                .createdAt(Instant.now())
                .build());
        var start = new LasVegasStartPhase(List.of(
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("P2", "Player 2", false, true),
                new PlayerStartInfo("P3", "Player 3", false, true)
        ), new ZeroRandom());
        start.enter();
        var runtime = start.transit();
        runtime.drainPublicEvents();
        String gameId = sessionId + ":LASVEGAS:r1";
        games.save(GameEntity.builder()
                .id(gameId)
                .sessionId(sessionId)
                .roundIndex(1)
                .gameType("LASVEGAS")
                .state("RUNNING")
                .stateJson(codec.encode(runtime))
                .createdAt(Instant.now())
                .build());

        CountDownLatch startGate = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            startGate.await(5, TimeUnit.SECONDS);
            try {
                service.command(gameId, "P1", new LasVegasRuntimePhase.Command(0, "ROLL_DICE", null));
                return true;
            } catch (GameStateConflictException exception) {
                return false;
            }
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(pool.submit(attempt), pool.submit(attempt));
            startGate.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(10, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
            assertEquals(1, codec.decode(games.findById(gameId).orElseThrow().getStateJson()).stateVersion());
        } finally {
            pool.shutdownNow();
        }
    }

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
