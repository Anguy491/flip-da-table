package com.flip.backend.conquerwesteros.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.ConquerWesterosTurnExecutor;
import com.flip.backend.conquerwesteros.engine.Campaign;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
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

class ConquerWesterosBotCoordinatorTest {
    @Test
    void recoverySchedulesEachSnapshotVersionOnlyOnce() {
        var codec = new ConquerWesterosSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var runtime = ConquerWesterosRuntimePhase.newGame(List.of(
                new PlayerStartInfo("BOT1", "Bot 1", true, true),
                new PlayerStartInfo("P1", "Player 1", false, true)
        ), Campaign.WAR_OF_FIVE_KINGS, new ZeroRandom());
        var entity = GameEntity.builder().id("game-1").gameType("CONQUERWESTEROS")
                .state("RUNNING").stateJson(codec.encode(runtime)).build();
        var scheduler = mock(TaskScheduler.class);
        var turns = mock(ConquerWesterosTurnExecutor.class);
        var games = mock(GameRepository.class);
        when(games.findByGameTypeIgnoreCaseAndState("CONQUERWESTEROS", "RUNNING")).thenReturn(List.of(entity));
        var coordinator = new ConquerWesterosBotCoordinator(
                scheduler, turns, games, codec, new SimpleMeterRegistry(), 0);

        coordinator.recoverInterruptedTurns();
        coordinator.recoverInterruptedTurns();

        var runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(1)).schedule(runnable.capture(), any(Instant.class));
        when(turns.executeBot(any())).thenReturn(new ConquerWesterosTurnExecutor.Outcome(
                false, Map.of(), List.of(), null));
        runnable.getValue().run();
        verify(turns).executeBot(runtime.botTicket("game-1"));
        assertEquals(0, runtime.stateVersion());
    }

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
