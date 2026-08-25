package com.flip.backend.lasvegas.engine;

import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Read-through runtime cache. Authoritative state remains games.state_json. */
@Component
public class LasVegasGameRegistry {
    private static final Logger log = LoggerFactory.getLogger(LasVegasGameRegistry.class);

    private final Map<String, LasVegasRuntimePhase> games = new ConcurrentHashMap<>();
    private final GameRepository repository;
    private final LasVegasSnapshotCodec codec;

    @Autowired
    public LasVegasGameRegistry(GameRepository repository, LasVegasSnapshotCodec codec, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.codec = codec;
        Gauge.builder("lasvegas.games.active", games, cached -> cached.values().stream()
                        .filter(runtime -> runtime.state() != LasVegasRuntimePhase.State.FINISHED)
                        .count())
                .description("Active Las Vegas aggregates in this runtime cache")
                .register(meterRegistry);
    }

    LasVegasGameRegistry(GameRepository repository, LasVegasSnapshotCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    public LasVegasRuntimePhase get(String gameId) {
        var cached = games.get(gameId);
        if (cached != null) return cached;
        GameEntity entity = repository.findById(gameId).orElse(null);
        if (entity == null || !"LASVEGAS".equalsIgnoreCase(entity.getGameType())) return null;
        return restore(entity);
    }

    /** Refresh a stale cache entry after the database row has been locked. */
    public LasVegasRuntimePhase getForUpdate(GameEntity entity) {
        LasVegasSnapshot snapshot = decode(entity);
        var cached = games.get(entity.getId());
        if (cached != null && cached.stateVersion() == snapshot.stateVersion()) return cached;
        var restored = LasVegasRuntimePhase.restore(snapshot);
        games.put(entity.getId(), restored);
        return restored;
    }

    public void put(String gameId, LasVegasRuntimePhase runtime) {
        games.put(gameId, runtime);
    }

    public void remove(String gameId) {
        games.remove(gameId);
    }

    private LasVegasRuntimePhase restore(GameEntity entity) {
        var restored = LasVegasRuntimePhase.restore(decode(entity));
        var existing = games.putIfAbsent(entity.getId(), restored);
        return existing == null ? restored : existing;
    }

    private LasVegasSnapshot decode(GameEntity entity) {
        try {
            return codec.decode(entity.getStateJson());
        } catch (RuntimeException exception) {
            io.micrometer.core.instrument.Metrics.counter("lasvegas.snapshot.deserialization.failures").increment();
            log.error("Las Vegas snapshot deserialization failed for game {}", entity.getId(), exception);
            throw exception;
        }
    }
}
