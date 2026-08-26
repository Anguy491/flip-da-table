package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.conquerwesteros.ConquerWesterosTurnExecutor;
import com.flip.backend.conquerwesteros.engine.Campaign;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosGameRegistry;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosStartPhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PublicEvent;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

@Service
public class ConquerWesterosGameService extends GameService {
    public record CommandOutcome(GameView view, List<PublicEvent> publicEvents) {}

    private final ConquerWesterosGameRegistry registry;
    private final ConquerWesterosSnapshotCodec codec;
    private final ConquerWesterosTurnExecutor turns;
    private final Supplier<Random> randomSupplier;

    @Autowired
    public ConquerWesterosGameService(
            SessionRepository sessions,
            GameRepository games,
            ConquerWesterosGameRegistry registry,
            ConquerWesterosSnapshotCodec codec,
            ConquerWesterosTurnExecutor turns
    ) {
        this(sessions, games, registry, codec, turns, SecureRandom::new);
    }

    ConquerWesterosGameService(
            SessionRepository sessions,
            GameRepository games,
            ConquerWesterosGameRegistry registry,
            ConquerWesterosSnapshotCodec codec,
            ConquerWesterosTurnExecutor turns,
            Supplier<Random> randomSupplier
    ) {
        super(sessions, games);
        this.registry = registry;
        this.codec = codec;
        this.turns = turns;
        this.randomSupplier = randomSupplier;
    }

    @Override public boolean supports(String gameType) { return "CONQUERWESTEROS".equalsIgnoreCase(gameType); }
    @Override public GameCapabilities capabilities() { return GameCapabilities.forGameType("CONQUERWESTEROS"); }

    @Override
    protected boolean isFinished(String gameId) {
        var runtime = registry.get(gameId);
        return runtime != null && runtime.state() == ConquerWesterosRuntimePhase.State.FINISHED;
    }

    @Override
    public Object viewFor(String gameId, String playerId) {
        return requireRuntime(gameId).buildView(playerId);
    }

    @Override
    @Transactional
    public StartGameResponse startFirst(String sessionId, StartGameRequest request) {
        var session = beginFirstRound(sessionId);
        if (!supports(session.getGameType())) throw new IllegalArgumentException("unsupported game type for Conquer Westeros service");
        Campaign campaign = validateStart(request);
        var base = persistRound(session, 1);
        var playerInfos = new ArrayList<PlayerStartInfo>();
        int index = 1;
        for (var spec : request.players()) {
            if (spec.name() != null && !spec.name().isBlank()) {
                playerInfos.add(new PlayerStartInfo("P" + index++, spec.name().trim(), false, spec.ready()));
            }
        }
        var start = new ConquerWesterosStartPhase(playerInfos, campaign, randomSupplier.get());
        start.enter();
        var runtime = start.transit();
        runtime.drainPublicEvents();
        var entity = games.findById(base.gameId()).orElseThrow();
        entity.setStateJson(codec.encode(runtime));
        games.save(entity);
        registry.put(base.gameId(), runtime);
        removeRegistryEntryAfterRollback(base.gameId());
        String myPlayerId = playerInfos.get(0).playerId();
        return new StartGameResponse(base.gameId(), 1, myPlayerId, List.copyOf(playerInfos), runtime.buildView(myPlayerId));
    }

    @Override
    public StartGameResponse startNext(String sessionId, StartGameRequest request) {
        throw new IllegalArgumentException("Conquer Westeros supports one complete game per room");
    }

    public CommandOutcome command(String gameId, String playerId, ConquerWesterosRuntimePhase.Command command) {
        var outcome = turns.execute(gameId, playerId, command);
        return new CommandOutcome(outcome.viewFor(playerId), outcome.publicEvents());
    }

    public ConquerWesterosRuntimePhase runtime(String gameId) { return requireRuntime(gameId); }

    private Campaign validateStart(StartGameRequest request) {
        if (request.rounds() != 1) throw new IllegalArgumentException("Conquer Westeros requires rounds=1");
        int count = countValidPlayers(request);
        if (count < capabilities().minPlayers() || count > capabilities().maxPlayers()) {
            throw new IllegalArgumentException("Conquer Westeros requires 2-6 players");
        }
        if (request.players().stream().anyMatch(spec -> spec.name() != null && !spec.name().isBlank() && spec.bot())) {
            throw new IllegalArgumentException("Conquer Westeros does not support bots");
        }
        return Campaign.parse(request.options().get("campaign"));
    }

    private ConquerWesterosRuntimePhase requireRuntime(String gameId) {
        var runtime = registry.get(gameId);
        if (runtime == null) throw new IllegalArgumentException("game not found");
        return runtime;
    }

    private void removeRegistryEntryAfterRollback(String gameId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) registry.remove(gameId);
            }
        });
    }
}
