package com.flip.backend.conquerwesteros;

import com.flip.backend.api.ConquerWesterosWsService;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosGameRegistry;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PublicEvent;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
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
    public record Outcome(Map<String, GameView> views, List<PublicEvent> publicEvents) {
        public GameView viewFor(String playerId) { return views.get(playerId); }
    }

    private final SessionRepository sessions;
    private final GameRepository games;
    private final ConquerWesterosGameRegistry registry;
    private final ConquerWesterosSnapshotCodec codec;
    private final ConquerWesterosWsService ws;

    public ConquerWesterosTurnExecutor(
            SessionRepository sessions,
            GameRepository games,
            ConquerWesterosGameRegistry registry,
            ConquerWesterosSnapshotCodec codec,
            ConquerWesterosWsService ws
    ) {
        this.sessions = sessions;
        this.games = games;
        this.registry = registry;
        this.codec = codec;
        this.ws = ws;
    }

    @Transactional
    public Outcome execute(String gameId, String playerId, ConquerWesterosRuntimePhase.Command command) {
        GameEntity entity = requireRunningGame(gameId);
        var runtime = registry.getForUpdate(entity);
        try {
            var batch = runtime.applyCommand(playerId, command);
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
                ws.broadcastViews(gameId, views);
                ws.broadcastEvents(gameId, batch.publicEvents());
            });
            return new Outcome(views, batch.publicEvents());
        } catch (RuntimeException exception) {
            registry.remove(gameId);
            throw exception;
        }
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
