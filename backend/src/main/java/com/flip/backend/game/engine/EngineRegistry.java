package com.flip.backend.game.engine;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry to locate engines by GameType. */
public final class EngineRegistry {
    private final Map<GameType, GameEngine<?, ?, ?>> engines = new ConcurrentHashMap<>();

    public <S, C extends Command, E extends Event> void register(GameType type, GameEngine<S, C, E> engine) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(engine, "engine");
        engines.put(type, engine);
    }

    @SuppressWarnings("unchecked")
    public <S, C extends Command, E extends Event> GameEngine<S, C, E> get(GameType type) {
        GameEngine<?, ?, ?> engine = engines.get(type);
        if (engine == null) throw new IllegalStateException("No engine registered for " + type);
        return (GameEngine<S, C, E>) engine;
    }

    public boolean contains(GameType type) {
        return engines.containsKey(type);
    }
}
