package com.flip.backend.conquerwesteros.engine;

import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Read-through cache. The authoritative aggregate remains games.state_json. */
@Component
public class ConquerWesterosGameRegistry {
    private static final Logger log = LoggerFactory.getLogger(ConquerWesterosGameRegistry.class);

    private final Map<String, ConquerWesterosRuntimePhase> games = new ConcurrentHashMap<>();
    private final GameRepository repository;
    private final ConquerWesterosSnapshotCodec codec;

    public ConquerWesterosGameRegistry(
            GameRepository repository,
            ConquerWesterosSnapshotCodec codec,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.codec = codec;
        Gauge.builder("conquerwesteros.games.active", games, cached -> cached.values().stream()
                        .filter(runtime -> runtime.state() != ConquerWesterosRuntimePhase.State.FINISHED)
                        .count())
                .description("Active Conquer Westeros aggregates in the runtime cache")
                .register(meterRegistry);
    }

    public ConquerWesterosRuntimePhase get(String gameId) {
        var cached = games.get(gameId);
        if (cached != null) return cached;
        GameEntity entity = repository.findById(gameId).orElse(null);
        if (entity == null || !"CONQUERWESTEROS".equalsIgnoreCase(entity.getGameType())) return null;
        return restore(entity);
    }

    public ConquerWesterosRuntimePhase getForUpdate(GameEntity entity) {
        ConquerWesterosSnapshot snapshot = decode(entity);
        var cached = games.get(entity.getId());
        if (cached != null && cached.stateVersion() == snapshot.stateVersion()) return cached;
        var restored = ConquerWesterosRuntimePhase.restore(snapshot);
        games.put(entity.getId(), restored);
        return restored;
    }

    public void put(String gameId, ConquerWesterosRuntimePhase runtime) { games.put(gameId, runtime); }
    public void remove(String gameId) { games.remove(gameId); }

    private ConquerWesterosRuntimePhase restore(GameEntity entity) {
        var restored = ConquerWesterosRuntimePhase.restore(decode(entity));
        var existing = games.putIfAbsent(entity.getId(), restored);
        return existing == null ? restored : existing;
    }

    private ConquerWesterosSnapshot decode(GameEntity entity) {
        try {
            return codec.decode(entity.getStateJson());
        } catch (RuntimeException exception) {
            io.micrometer.core.instrument.Metrics.counter("conquerwesteros.snapshot.deserialization.failures").increment();
            log.error("Conquer Westeros snapshot deserialization failed for game {}", entity.getId(), exception);
            throw exception;
        }
    }
}
