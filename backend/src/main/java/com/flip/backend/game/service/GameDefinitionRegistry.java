package com.flip.backend.game.service;

import com.flip.backend.game.engine.GameType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry for game definitions (state class + engine + projector). */
public class GameDefinitionRegistry {
    private final Map<GameType, GameDefinition<?, ?, ?, ?>> defs = new ConcurrentHashMap<>();

    public void register(GameDefinition<?, ?, ?, ?> def) {
        defs.put(def.type(), def);
    }

    @SuppressWarnings("unchecked")
    public <S> GameDefinition<S, ?, ?, ?> get(GameType type) {
        GameDefinition<?, ?, ?, ?> def = defs.get(type);
        if (def == null) throw new IllegalStateException("No game definition for " + type);
        return (GameDefinition<S, ?, ?, ?>) def;
    }
}
