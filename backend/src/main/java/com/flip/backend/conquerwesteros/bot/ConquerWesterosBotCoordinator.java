package com.flip.backend.conquerwesteros.bot;

import com.flip.backend.conquerwesteros.ConquerWesterosTurnExecutor;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshot;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
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

/** Schedules one guarded Bot command at a time and repairs interrupted Bot turns. */
@Component
public class ConquerWesterosBotCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ConquerWesterosBotCoordinator.class);

    private final TaskScheduler scheduler;
    private final ConquerWesterosTurnExecutor turns;
    private final GameRepository games;
    private final ConquerWesterosSnapshotCodec codec;
    private final MeterRegistry meters;
    private final long stepDelayMs;
    private final Set<ConquerWesterosBotTicket> pending = ConcurrentHashMap.newKeySet();

    public ConquerWesterosBotCoordinator(
            TaskScheduler scheduler,
            ConquerWesterosTurnExecutor turns,
            GameRepository games,
            ConquerWesterosSnapshotCodec codec,
            MeterRegistry meters,
            @Value("${app.conquer-westeros.bot.step-delay-ms:800}") long stepDelayMs
    ) {
        this.scheduler = scheduler;
        this.turns = turns;
        this.games = games;
        this.codec = codec;
        this.meters = meters;
        this.stepDelayMs = Math.max(0, stepDelayMs);
    }

    public void schedule(ConquerWesterosBotTicket ticket) {
        if (ticket == null || !pending.add(ticket)) return;
        try {
            scheduler.schedule(() -> run(ticket), Instant.now().plusMillis(stepDelayMs));
        } catch (RuntimeException exception) {
            pending.remove(ticket);
            throw exception;
        }
    }

    private void run(ConquerWesterosBotTicket ticket) {
        pending.remove(ticket);
        try {
            var outcome = turns.executeBot(ticket);
            schedule(outcome.nextBotTicket());
        } catch (RuntimeException exception) {
            meters.counter("conquerwesteros.bot.actions", "type", "failure").increment();
            log.error("Conquer Westeros Bot step failed for game {} at version {}",
                    ticket.gameId(), ticket.expectedVersion(), exception);
        }
    }

    @Scheduled(fixedDelayString = "${app.conquer-westeros.bot.recovery-interval-ms:5000}")
    public void recoverInterruptedTurns() {
        for (var entity : games.findByGameTypeIgnoreCaseAndState("CONQUERWESTEROS", "RUNNING")) {
            try {
                ConquerWesterosSnapshot snapshot = codec.decode(entity.getStateJson());
                ConquerWesterosSnapshot.PlayerState current = snapshot.players().stream()
                        .filter(player -> player.playerId().equals(snapshot.currentPlayerId()))
                        .findFirst().orElse(null);
                ConquerWesterosRuntimePhase.State phase = ConquerWesterosRuntimePhase.State.valueOf(snapshot.phase());
                if (current != null && current.bot()
                        && (phase == ConquerWesterosRuntimePhase.State.WAITING_FOR_ROLL
                        || phase == ConquerWesterosRuntimePhase.State.WAITING_FOR_DECISION)) {
                    schedule(new ConquerWesterosBotTicket(
                            entity.getId(), snapshot.stateVersion(), phase, current.playerId()));
                }
            } catch (RuntimeException exception) {
                log.error("Conquer Westeros Bot recovery could not inspect game {}", entity.getId(), exception);
            }
        }
    }
}
