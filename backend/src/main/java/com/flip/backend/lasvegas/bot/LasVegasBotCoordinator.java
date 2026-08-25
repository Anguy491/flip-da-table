package com.flip.backend.lasvegas.bot;

import com.flip.backend.lasvegas.engine.LasVegasSnapshot;
import com.flip.backend.lasvegas.engine.LasVegasSnapshotCodec;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.persistence.GameRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Schedules one guarded bot command at a time and repairs interrupted bot turns. */
@Component
public class LasVegasBotCoordinator {
    private static final Logger log = LoggerFactory.getLogger(LasVegasBotCoordinator.class);

    private final TaskScheduler scheduler;
    private final LasVegasTurnExecutor turns;
    private final GameRepository games;
    private final LasVegasSnapshotCodec codec;
    private final MeterRegistry meters;
    private final long stepDelayMs;
    private final Set<LasVegasBotTicket> pending = ConcurrentHashMap.newKeySet();

    public LasVegasBotCoordinator(
            TaskScheduler scheduler,
            LasVegasTurnExecutor turns,
            GameRepository games,
            LasVegasSnapshotCodec codec,
            MeterRegistry meters,
            @Value("${app.las-vegas.bot.step-delay-ms:800}") long stepDelayMs
    ) {
        this.scheduler = scheduler;
        this.turns = turns;
        this.games = games;
        this.codec = codec;
        this.meters = meters;
        this.stepDelayMs = Math.max(0, stepDelayMs);
    }

    public void schedule(LasVegasBotTicket ticket) {
        if (ticket == null || !pending.add(ticket)) return;
        try {
            scheduler.schedule(() -> run(ticket), Instant.now().plusMillis(stepDelayMs));
        } catch (RuntimeException exception) {
            pending.remove(ticket);
            throw exception;
        }
    }

    private void run(LasVegasBotTicket ticket) {
        pending.remove(ticket);
        try {
            var outcome = turns.executeBot(ticket);
            schedule(outcome.nextBotTicket());
        } catch (RuntimeException exception) {
            meters.counter("lasvegas.bot.actions", "type", "failure").increment();
            log.error("Las Vegas bot step failed for game {} at version {}", ticket.gameId(), ticket.expectedVersion(), exception);
        }
    }

    @Scheduled(fixedDelayString = "${app.las-vegas.bot.recovery-interval-ms:5000}")
    public void recoverInterruptedTurns() {
        for (var entity : games.findByGameTypeIgnoreCaseAndState("LASVEGAS", "RUNNING")) {
            try {
                LasVegasSnapshot snapshot = codec.decode(entity.getStateJson());
                LasVegasSnapshot.PlayerState current = snapshot.players().stream()
                        .filter(player -> player.playerId().equals(snapshot.currentPlayerId()))
                        .findFirst()
                        .orElse(null);
                LasVegasRuntimePhase.State phase = LasVegasRuntimePhase.State.valueOf(snapshot.phase());
                if (current != null && current.bot()
                        && (phase == LasVegasRuntimePhase.State.WAITING_FOR_ROLL
                        || phase == LasVegasRuntimePhase.State.WAITING_FOR_CHOICE)) {
                    schedule(new LasVegasBotTicket(entity.getId(), snapshot.stateVersion(), phase, current.playerId()));
                }
            } catch (RuntimeException exception) {
                log.error("Las Vegas bot recovery could not inspect game {}", entity.getId(), exception);
            }
        }
    }
}
