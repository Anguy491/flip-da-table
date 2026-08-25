package com.flip.backend.lasvegas.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.lasvegas.engine.LasVegasSnapshotCodec;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LasVegasBotCoordinatorTest {
    @Test
    void recoverySchedulesEachSnapshotVersionOnlyOnce() {
        var codec = new LasVegasSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var runtime = LasVegasRuntimePhase.newGame(List.of(
                new PlayerStartInfo("BOT1", "Bot 1", true, true),
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("BOT2", "Bot 2", true, true)
        ), new ZeroRandom());
        var entity = GameEntity.builder()
                .id("game-1")
                .gameType("LASVEGAS")
                .state("RUNNING")
                .stateJson(codec.encode(runtime))
                .build();
        var scheduler = mock(TaskScheduler.class);
        var turns = mock(LasVegasTurnExecutor.class);
        var games = mock(GameRepository.class);
        when(games.findByGameTypeIgnoreCaseAndState("LASVEGAS", "RUNNING")).thenReturn(List.of(entity));
        var coordinator = new LasVegasBotCoordinator(scheduler, turns, games, codec, new SimpleMeterRegistry(), 0);

        coordinator.recoverInterruptedTurns();
        coordinator.recoverInterruptedTurns();

        var runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(1)).schedule(runnable.capture(), any(Instant.class));
        var next = new LasVegasTurnExecutor.Outcome(false, Map.of(), List.of(), null);
        when(turns.executeBot(any())).thenReturn(next);
        runnable.getValue().run();
        verify(turns).executeBot(runtime.botTicket("game-1"));
        assertEquals(0, runtime.stateVersion());
    }

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
