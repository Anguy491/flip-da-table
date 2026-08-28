package com.flip.backend.conquerwesteros;

import com.flip.backend.api.ConquerWesterosWsService;
import com.flip.backend.conquerwesteros.bot.ConquerWesterosBotStrategy;
import com.flip.backend.conquerwesteros.bot.ConquerWesterosBotTicket;
import com.flip.backend.conquerwesteros.bot.StandardConquerWesterosBotStrategy;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosGameRegistry;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PublicEvent;
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

/** Row-locked command path with post-commit view and event broadcasts. */
@Service
public class ConquerWesterosTurnExecutor {
    public record Outcome(
            boolean applied,
            Map<String, GameView> views,
            List<PublicEvent> publicEvents,
            ConquerWesterosBotTicket nextBotTicket
    ) {
        public GameView viewFor(String playerId) { return views.get(playerId); }
    }

    private final SessionRepository sessions;
    private final GameRepository games;
    private final ConquerWesterosGameRegistry registry;
    private final ConquerWesterosSnapshotCodec codec;
    private final ConquerWesterosWsService ws;
    private final ConquerWesterosBotStrategy strategy;
    private final MeterRegistry meters;

    public ConquerWesterosTurnExecutor(
            SessionRepository sessions,
            GameRepository games,
            ConquerWesterosGameRegistry registry,
            ConquerWesterosSnapshotCodec codec,
            ConquerWesterosWsService ws,
            ConquerWesterosBotStrategy strategy,
            MeterRegistry meters
    ) {
        this.sessions = sessions;
        this.games = games;
        this.registry = registry;
        this.codec = codec;
        this.ws = ws;
        this.strategy = strategy;
        this.meters = meters;
    }

    @Transactional
    public Outcome execute(String gameId, String playerId, ConquerWesterosRuntimePhase.Command command) {
        GameEntity entity = requireRunningGame(gameId);
        var runtime = registry.getForUpdate(entity);
        try {
            return finishMutation(entity, runtime, runtime.applyCommand(playerId, command));
        } catch (RuntimeException exception) {
            registry.remove(gameId);
            throw exception;
        }
    }

    @Transactional
    public Outcome executeBot(ConquerWesterosBotTicket ticket) {
        GameEntity entity = requireRunningGame(ticket.gameId());
        var runtime = registry.getForUpdate(entity);
        if (runtime.stateVersion() != ticket.expectedVersion()
                || runtime.state() != ticket.expectedPhase()
                || !runtime.currentPlayerIsBot()
                || !runtime.currentPlayerId().equals(ticket.botId())) {
            meters.counter("conquerwesteros.bot.tickets", "outcome", "stale").increment();
            return new Outcome(false, Map.of(), List.of(), runtime.botTicket(ticket.gameId()));
        }

        ConquerWesterosRuntimePhase.Command command;
        if (runtime.state() == ConquerWesterosRuntimePhase.State.WAITING_FOR_ROLL) {
            command = new ConquerWesterosRuntimePhase.Command(runtime.stateVersion(), "ROLL_DICE",
                    null, null, List.of(), null);
        } else {
            ConquerWesterosBotStrategy.Decision decision;
            var turnState = runtime.botTurnState();
            try {
                decision = strategy.choose(turnState);
            } catch (RuntimeException exception) {
                meters.counter("conquerwesteros.bot.strategy", "outcome", "failure").increment();
                decision = StandardConquerWesterosBotStrategy.fallback(turnState);
            }
            if (!runtime.isLegalBotDecision(decision)) {
                meters.counter("conquerwesteros.bot.strategy", "outcome", "illegal").increment();
                decision = StandardConquerWesterosBotStrategy.fallback(turnState);
            }
            command = new ConquerWesterosRuntimePhase.Command(runtime.stateVersion(), decision.type(),
                    decision.targetId(), decision.lineId(), decision.dieIds(), decision.dieId());
        }
        try {
            Outcome outcome = finishMutation(entity, runtime, runtime.applyCommand(ticket.botId(), command));
            meters.counter("conquerwesteros.bot.actions", "type", command.type()).increment();
            return outcome;
        } catch (RuntimeException exception) {
            registry.remove(ticket.gameId());
            throw exception;
        }
    }

    private Outcome finishMutation(
            GameEntity entity,
            ConquerWesterosRuntimePhase runtime,
            ConquerWesterosRuntimePhase.CommandBatch batch
    ) {
        entity.setStateJson(codec.encode(runtime));
        if (runtime.state() == ConquerWesterosRuntimePhase.State.FINISHED) {
            entity.setState("ENDED");
            var session = sessions.findByIdForUpdate(entity.getSessionId()).orElseThrow();
            session.setState("ENDED");
            sessions.save(session);
        }
        games.save(entity);
        Map<String, GameView> views = viewsForAll(runtime);
        afterCommit(() -> {
            ws.broadcastViews(entity.getId(), views);
            ws.broadcastEvents(entity.getId(), batch.publicEvents());
        });
        return new Outcome(true, views, batch.publicEvents(), runtime.botTicket(entity.getId()));
    }

    private GameEntity requireRunningGame(String gameId) {
        var entity = games.findByIdForUpdate(gameId)
                .orElseThrow(() -> new IllegalArgumentException("game not found"));
        if (!"CONQUERWESTEROS".equalsIgnoreCase(entity.getGameType())) {
            throw new IllegalArgumentException("game is not Conquer Westeros");
        }
        if (!"RUNNING".equals(entity.getState())) throw new IllegalArgumentException("game has ended");
        return entity;
    }

    private Map<String, GameView> viewsForAll(ConquerWesterosRuntimePhase runtime) {
        var views = new LinkedHashMap<String, GameView>();
        for (String id : runtime.playerIds()) views.put(id, runtime.buildView(id));
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
