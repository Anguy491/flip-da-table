package com.flip.backend.service.game;

import com.flip.backend.api.LasVegasWsService;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.lasvegas.LasVegasPresentationService;
import com.flip.backend.lasvegas.engine.LasVegasGameRegistry;
import com.flip.backend.lasvegas.engine.LasVegasSnapshotCodec;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.engine.phase.LasVegasStartPhase;
import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

@Service
public class LasVegasGameService extends GameService {
    public record CommandOutcome(GameView view, List<PublicEvent> publicEvents) {}
    public record PresentationOutcome(GameView view, PublicEvent publicEvent) {}

    private final LasVegasGameRegistry registry;
    private final LasVegasSnapshotCodec codec;
    private final LasVegasPresentationService presentation;
    private final LasVegasWsService ws;
    private final Supplier<Random> randomSupplier;

    @Autowired
    public LasVegasGameService(
            SessionRepository sessions,
            GameRepository games,
            LasVegasGameRegistry registry,
            LasVegasSnapshotCodec codec,
            LasVegasPresentationService presentation,
            LasVegasWsService ws
    ) {
        this(sessions, games, registry, codec, presentation, ws, SecureRandom::new);
    }

    LasVegasGameService(
            SessionRepository sessions,
            GameRepository games,
            LasVegasGameRegistry registry,
            LasVegasSnapshotCodec codec,
            LasVegasPresentationService presentation,
            LasVegasWsService ws,
            Supplier<Random> randomSupplier
    ) {
        super(sessions, games);
        this.registry = registry;
        this.codec = codec;
        this.presentation = presentation;
        this.ws = ws;
        this.randomSupplier = randomSupplier;
    }

    @Override public boolean supports(String gameType) { return "LASVEGAS".equalsIgnoreCase(gameType); }
    @Override public GameCapabilities capabilities() { return GameCapabilities.forGameType("LASVEGAS"); }

    @Override
    protected boolean isFinished(String gameId) {
        var runtime = registry.get(gameId);
        return runtime != null && runtime.state() == LasVegasRuntimePhase.State.FINISHED;
    }

    @Override
    public Object viewFor(String gameId, String playerId) {
        var runtime = requireRuntime(gameId);
        return runtime.buildView(playerId, presentation.totals(gameId, runtime));
    }

    @Override
    @Transactional
    public StartGameResponse startFirst(String sessionId, StartGameRequest request) {
        var session = beginFirstRound(sessionId);
        if (!supports(session.getGameType())) throw new IllegalArgumentException("unsupported game type for Las Vegas service");
        validateStart(request);

        var base = persistRound(session, 1);
        var playerInfos = new java.util.ArrayList<PlayerStartInfo>();
        int index = 1;
        for (var spec : request.players()) {
            if (spec.name() == null || spec.name().isBlank()) continue;
            playerInfos.add(new PlayerStartInfo("P" + index++, spec.name().trim(), false, spec.ready()));
        }

        var start = new LasVegasStartPhase(playerInfos, randomSupplier.get());
        start.enter();
        var runtime = start.transit();
        runtime.drainPublicEvents(); // The initial view already represents ROUND_STARTED.
        var entity = games.findById(base.gameId()).orElseThrow();
        entity.setStateJson(codec.encode(runtime));
        games.save(entity);
        registry.put(base.gameId(), runtime);
        removeRegistryEntryAfterRollback(base.gameId());

        String myPlayerId = playerInfos.get(0).playerId();
        var view = runtime.buildView(myPlayerId, Map.of());
        return new StartGameResponse(base.gameId(), 1, myPlayerId, List.copyOf(playerInfos), view);
    }

    @Override
    public StartGameResponse startNext(String sessionId, StartGameRequest request) {
        throw new IllegalArgumentException("Las Vegas is one platform game with three internal rounds");
    }

    @Transactional
    public CommandOutcome command(String gameId, String playerId, LasVegasRuntimePhase.Command command) {
        var entity = games.findByIdForUpdate(gameId)
                .orElseThrow(() -> new IllegalArgumentException("game not found"));
        if (!supports(entity.getGameType())) throw new IllegalArgumentException("game is not Las Vegas");
        if (!"RUNNING".equals(entity.getState())) throw new IllegalArgumentException("game has ended");
        var runtime = registry.getForUpdate(entity);
        try {
            var batch = runtime.applyCommand(playerId, command);
            entity.setStateJson(codec.encode(runtime));
            boolean finished = runtime.state() == LasVegasRuntimePhase.State.FINISHED;
            if (finished) {
                entity.setState("ENDED");
                var session = sessions.findByIdForUpdate(entity.getSessionId()).orElseThrow();
                session.setState("ENDED");
                sessions.save(session);
                presentation.clear(gameId);
            }
            games.save(entity);

            Map<String, Integer> totals = presentation.totals(gameId, runtime);
            Map<String, GameView> views = viewsForAll(runtime, totals);
            GameView responseView = views.get(playerId);
            afterCommit(() -> {
                ws.broadcastViews(gameId, views);
                ws.broadcastEvents(gameId, batch.publicEvents());
            });
            return new CommandOutcome(responseView, batch.publicEvents());
        } catch (RuntimeException exception) {
            registry.remove(gameId);
            throw exception;
        }
    }

    public PresentationOutcome setAssetVisibility(String gameId, String playerId, boolean visible) {
        var runtime = requireRuntime(gameId);
        synchronized (runtime) {
            PublicEvent event = presentation.setVisible(gameId, playerId, visible, runtime);
            Map<String, Integer> totals = presentation.totals(gameId, runtime);
            Map<String, GameView> views = viewsForAll(runtime, totals);
            ws.broadcastViews(gameId, views);
            ws.broadcastEvents(gameId, List.of(event));
            return new PresentationOutcome(views.get(playerId), event);
        }
    }

    public LasVegasRuntimePhase runtime(String gameId) {
        return requireRuntime(gameId);
    }

    private void validateStart(StartGameRequest request) {
        if (request.rounds() != 1) throw new IllegalArgumentException("Las Vegas requires rounds=1");
        int count = countValidPlayers(request);
        if (count < capabilities().minPlayers() || count > capabilities().maxPlayers()) {
            throw new IllegalArgumentException("Las Vegas requires 3-10 players");
        }
        if (request.players().stream().anyMatch(spec -> spec.bot())) {
            throw new IllegalArgumentException("Las Vegas does not allow bots");
        }
    }

    private LasVegasRuntimePhase requireRuntime(String gameId) {
        var runtime = registry.get(gameId);
        if (runtime == null) throw new IllegalArgumentException("game not found");
        return runtime;
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

    private void removeRegistryEntryAfterRollback(String gameId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) registry.remove(gameId);
            }
        });
    }
}
