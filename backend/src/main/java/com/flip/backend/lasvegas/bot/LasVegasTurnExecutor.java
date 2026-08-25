package com.flip.backend.lasvegas.bot;

import com.flip.backend.api.LasVegasWsService;
import com.flip.backend.lasvegas.LasVegasPresentationService;
import com.flip.backend.lasvegas.engine.LasVegasGameRegistry;
import com.flip.backend.lasvegas.engine.LasVegasSnapshotCodec;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared transactional command path for authenticated humans and delayed server-side bots. */
@Service
public class LasVegasTurnExecutor {
    public record Outcome(
            boolean applied,
            Map<String, GameView> views,
            List<PublicEvent> publicEvents,
            LasVegasBotTicket nextBotTicket
    ) {
        public GameView viewFor(String playerId) { return views.get(playerId); }
    }

    private final SessionRepository sessions;
    private final GameRepository games;
    private final LasVegasGameRegistry registry;
    private final LasVegasSnapshotCodec codec;
    private final LasVegasPresentationService presentation;
    private final LasVegasWsService ws;
    private final LasVegasBotStrategy strategy;
    private final MeterRegistry meters;

    public LasVegasTurnExecutor(
            SessionRepository sessions,
            GameRepository games,
            LasVegasGameRegistry registry,
            LasVegasSnapshotCodec codec,
            LasVegasPresentationService presentation,
            LasVegasWsService ws,
            LasVegasBotStrategy strategy,
            MeterRegistry meters
    ) {
        this.sessions = sessions;
        this.games = games;
        this.registry = registry;
        this.codec = codec;
        this.presentation = presentation;
        this.ws = ws;
        this.strategy = strategy;
        this.meters = meters;
    }

    @Transactional
    public Outcome executeHuman(String gameId, String playerId, LasVegasRuntimePhase.Command command) {
        return executeLocked(gameId, runtime -> runtime.applyCommand(playerId, command));
    }

    @Transactional
    public Outcome executeBot(LasVegasBotTicket ticket) {
        GameEntity entity = requireRunningGame(ticket.gameId());
        var runtime = registry.getForUpdate(entity);
        if (runtime.stateVersion() != ticket.expectedVersion()
                || runtime.state() != ticket.expectedPhase()
                || !runtime.currentPlayerIsBot()
                || !runtime.currentPlayerId().equals(ticket.botId())) {
            meters.counter("lasvegas.bot.tickets", "outcome", "stale").increment();
            return new Outcome(false, Map.of(), List.of(), runtime.botTicket(ticket.gameId()));
        }

        LasVegasRuntimePhase.Command command;
        if (runtime.state() == LasVegasRuntimePhase.State.WAITING_FOR_ROLL) {
            command = new LasVegasRuntimePhase.Command(runtime.stateVersion(), "ROLL_DICE", null);
        } else {
            LasVegasBotStrategy.Decision decision;
            try {
                decision = strategy.choose(runtime.botTurnState());
            } catch (RuntimeException exception) {
                meters.counter("lasvegas.bot.strategy", "outcome", "failure").increment();
                decision = LasVegasBotStrategy.Decision.place(runtime.lowestLegalFace());
            }
            if (!runtime.isLegalBotDecision(decision)) {
                meters.counter("lasvegas.bot.strategy", "outcome", "illegal").increment();
                decision = LasVegasBotStrategy.Decision.place(runtime.lowestLegalFace());
            }
            command = new LasVegasRuntimePhase.Command(runtime.stateVersion(), decision.type(), decision.face());
        }
        try {
            var outcome = finishMutation(entity, runtime, runtime.applyCommand(ticket.botId(), command));
            meters.counter("lasvegas.bot.actions", "type", command.type()).increment();
            return outcome;
        } catch (RuntimeException exception) {
            registry.remove(ticket.gameId());
            throw exception;
        }
    }

    private Outcome executeLocked(
            String gameId,
            java.util.function.Function<LasVegasRuntimePhase, LasVegasRuntimePhase.CommandBatch> mutation
    ) {
        GameEntity entity = requireRunningGame(gameId);
        var runtime = registry.getForUpdate(entity);
        try {
            return finishMutation(entity, runtime, mutation.apply(runtime));
        } catch (RuntimeException exception) {
            registry.remove(gameId);
            throw exception;
        }
    }

    private Outcome finishMutation(
            GameEntity entity,
            LasVegasRuntimePhase runtime,
            LasVegasRuntimePhase.CommandBatch batch
    ) {
        entity.setStateJson(codec.encode(runtime));
        if (runtime.state() == LasVegasRuntimePhase.State.FINISHED) {
            entity.setState("ENDED");
            var session = sessions.findByIdForUpdate(entity.getSessionId()).orElseThrow();
            session.setState("ENDED");
            sessions.save(session);
            presentation.clear(entity.getId());
        }
        games.save(entity);

        Map<String, Integer> totals = presentation.totals(entity.getId(), runtime);
        Map<String, GameView> views = viewsForAll(runtime, totals);
        List<PublicEvent> events = batch.publicEvents();
        afterCommit(() -> {
            ws.broadcastViews(entity.getId(), views);
            ws.broadcastEvents(entity.getId(), events);
        });
        return new Outcome(true, views, events, runtime.botTicket(entity.getId()));
    }

    private GameEntity requireRunningGame(String gameId) {
        var entity = games.findByIdForUpdate(gameId)
                .orElseThrow(() -> new IllegalArgumentException("game not found"));
        if (!"LASVEGAS".equalsIgnoreCase(entity.getGameType())) throw new IllegalArgumentException("game is not Las Vegas");
        if (!"RUNNING".equals(entity.getState())) throw new IllegalArgumentException("game has ended");
        return entity;
    }

    private Map<String, GameView> viewsForAll(LasVegasRuntimePhase runtime, Map<String, Integer> totals) {
        var views = new LinkedHashMap<String, GameView>();
        for (String id : runtime.playerIds()) views.put(id, runtime.buildView(id, totals));
        return Map.copyOf(views);
    }

    private void afterCommit(Runnable callback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            callback.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { callback.run(); }
        });
    }
}
