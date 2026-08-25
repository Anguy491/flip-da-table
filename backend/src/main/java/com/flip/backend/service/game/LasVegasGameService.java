package com.flip.backend.service.game;

import com.flip.backend.api.LasVegasWsService;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.lasvegas.LasVegasPresentationService;
import com.flip.backend.lasvegas.bot.LasVegasBotCoordinator;
import com.flip.backend.lasvegas.bot.LasVegasTurnExecutor;
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
    private final LasVegasTurnExecutor turns;
    private final LasVegasBotCoordinator bots;

    @Autowired
    public LasVegasGameService(
            SessionRepository sessions,
            GameRepository games,
            LasVegasGameRegistry registry,
            LasVegasSnapshotCodec codec,
            LasVegasPresentationService presentation,
            LasVegasWsService ws,
            LasVegasTurnExecutor turns,
            LasVegasBotCoordinator bots
    ) {
        this(sessions, games, registry, codec, presentation, ws, SecureRandom::new, turns, bots);
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
        this(sessions, games, registry, codec, presentation, ws, randomSupplier, null, null);
    }

    LasVegasGameService(
            SessionRepository sessions,
            GameRepository games,
            LasVegasGameRegistry registry,
            LasVegasSnapshotCodec codec,
            LasVegasPresentationService presentation,
            LasVegasWsService ws,
            Supplier<Random> randomSupplier,
            LasVegasTurnExecutor turns,
            LasVegasBotCoordinator bots
    ) {
        super(sessions, games);
        this.registry = registry;
        this.codec = codec;
        this.presentation = presentation;
        this.ws = ws;
        this.randomSupplier = randomSupplier;
        this.turns = turns;
        this.bots = bots;
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
        var view = runtime.buildView(playerId, presentation.totals(gameId, runtime));
        if (bots != null) {
            var ticket = runtime.botTicket(gameId);
            afterCommit(() -> bots.schedule(ticket));
        }
        return view;
    }

    @Override
    @Transactional
    public StartGameResponse startFirst(String sessionId, StartGameRequest request) {
        var session = beginFirstRound(sessionId);
        if (!supports(session.getGameType())) throw new IllegalArgumentException("unsupported game type for Las Vegas service");
        validateStart(request);

        var base = persistRound(session, 1);
        var playerInfos = new java.util.ArrayList<PlayerStartInfo>();
        int humanIndex = 1;
        for (var spec : request.players()) {
            if (spec.name() != null && !spec.name().isBlank() && !spec.bot()) {
                playerInfos.add(new PlayerStartInfo("P" + humanIndex++, spec.name().trim(), false, spec.ready()));
            }
        }
        int botIndex = 1;
        for (var spec : request.players()) {
            if (spec.name() != null && !spec.name().isBlank() && spec.bot()) {
                playerInfos.add(new PlayerStartInfo("BOT" + botIndex, "Bot " + botIndex, true, true));
                botIndex++;
            }
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

        String myPlayerId = playerInfos.stream().filter(player -> !player.bot())
                .map(PlayerStartInfo::playerId).findFirst().orElseThrow();
        var view = runtime.buildView(myPlayerId, Map.of());
        afterCommit(() -> { if (bots != null) bots.schedule(runtime.botTicket(base.gameId())); });
        return new StartGameResponse(base.gameId(), 1, myPlayerId, List.copyOf(playerInfos), view);
    }

    @Override
    public StartGameResponse startNext(String sessionId, StartGameRequest request) {
        throw new IllegalArgumentException("Las Vegas is one platform game with three internal rounds");
    }

    public CommandOutcome command(String gameId, String playerId, LasVegasRuntimePhase.Command command) {
        if (turns == null) throw new IllegalStateException("Las Vegas turn executor is unavailable");
        var outcome = turns.executeHuman(gameId, playerId, command);
        if (bots != null) bots.schedule(outcome.nextBotTicket());
        return new CommandOutcome(outcome.viewFor(playerId), outcome.publicEvents());
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
        long humans = request.players().stream()
                .filter(spec -> spec.name() != null && !spec.name().isBlank() && !spec.bot())
                .count();
        if (humans < 1) throw new IllegalArgumentException("Las Vegas requires at least one human player");
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
