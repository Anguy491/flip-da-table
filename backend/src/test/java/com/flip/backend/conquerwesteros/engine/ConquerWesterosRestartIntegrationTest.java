package com.flip.backend.conquerwesteros.engine;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosStartPhase;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.security.GameStateConflictException;
import com.flip.backend.service.game.ConquerWesterosGameService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "app.jwt.secret=01234567890123456789012345678901",
        "app.auth.password-reset.enabled=false",
        "app.auth.google.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class ConquerWesterosRestartIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired SessionRepository sessions;
    @Autowired GameRepository games;
    @Autowired ConquerWesterosSnapshotCodec codec;
    @Autowired ConquerWesterosGameService service;

    @Test
    void restoresAnInProgressRollFromTheAuthoritativeSnapshotAfterCacheLoss() {
        String sessionId = "33333333-3333-3333-3333-333333333333";
        String gameId = sessionId + ":CONQUERWESTEROS:r1";
        saveSession(sessionId);
        var runtime = runtime();
        runtime.applyCommand("P1", new ConquerWesterosRuntimePhase.Command(0, "ROLL_DICE", null, null, List.of(), null));
        ConquerWesterosSnapshot expected = runtime.snapshot();
        saveGame(gameId, sessionId, runtime);

        var restarted = new ConquerWesterosGameRegistry(games, codec, new SimpleMeterRegistry()).get(gameId);

        assertNotNull(restarted);
        assertEquals(expected, restarted.snapshot());
        assertEquals("WAITING_FOR_DECISION", restarted.buildView("P1").phase());
        assertEquals(7, restarted.buildView("P1").currentRoll().size());
    }

    @Test
    void databaseRowLockAllowsOnlyOneCommandAtTheSameVersion() throws Exception {
        String sessionId = "44444444-4444-4444-4444-444444444444";
        String gameId = sessionId + ":CONQUERWESTEROS:r1";
        saveSession(sessionId);
        saveGame(gameId, sessionId, runtime());

        CountDownLatch startGate = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            startGate.await(5, TimeUnit.SECONDS);
            try {
                service.command(gameId, "P1", new ConquerWesterosRuntimePhase.Command(0, "ROLL_DICE", null, null, List.of(), null));
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

    private ConquerWesterosRuntimePhase runtime() {
        var start = new ConquerWesterosStartPhase(List.of(
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("P2", "Player 2", false, true)
        ), Campaign.WAR_OF_FIVE_KINGS, new ZeroRandom());
        start.enter();
        var runtime = start.transit();
        runtime.drainPublicEvents();
        return runtime;
    }

    private void saveSession(String sessionId) {
        sessions.save(SessionEntity.builder()
                .id(sessionId)
                .ownerId(1L)
                .gameType("CONQUERWESTEROS")
                .maxPlayers(6)
                .state("RUNNING")
                .createdAt(Instant.now())
                .build());
    }

    private void saveGame(String gameId, String sessionId, ConquerWesterosRuntimePhase runtime) {
        games.save(GameEntity.builder()
                .id(gameId)
                .sessionId(sessionId)
                .roundIndex(1)
                .gameType("CONQUERWESTEROS")
                .state("RUNNING")
                .stateJson(codec.encode(runtime))
                .createdAt(Instant.now())
                .build());
    }

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
