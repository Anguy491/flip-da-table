package com.flip.backend.game.service;

import com.flip.backend.game.engine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Core game engine orchestrator (validate -> decide -> apply -> persist & definition registry). */
@Service("gameEngineService")
public class GameEngineService {
    private final GameJpaRepository repo;
    private final GameStateSerializer serializer;
    private final GameDefinitionRegistry definitionRegistry;

    public GameEngineService(GameJpaRepository repo, GameStateSerializer serializer) {
        this.repo = repo;
        this.serializer = serializer;
        this.definitionRegistry = new GameDefinitionRegistry(); // could be configured via @Bean
    }

    /** Register a game definition (invoked at startup configuration). */
    public void register(GameDefinition<?, ?, ?, ?> def) {
        definitionRegistry.register(def);
    }

    @Transactional(readOnly = true)
    public <S> S loadState(String gameId, GameType type, Class<S> stateClass) {
        GameEngineEntity e = repo.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        if (!Objects.equals(e.getGameType(), type.name())) {
            throw new IllegalStateException("Game type mismatch, expected " + type + " got " + e.getGameType());
        }
        return serializer.deserialize(e.getState(), stateClass);
    }

    @Transactional
    public <S, C extends Command, E extends Event> CommandResult<S, E> handleCommand(String gameId, C command) {
        GameEngineEntity entity = repo.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        GameType type = GameType.valueOf(entity.getGameType());
        GameDefinition<S, C, E, ?> def = cast(definitionRegistry.get(type));
        S state = serializer.deserialize(entity.getState(), def.stateClass());
        GameEngine<S, C, E> engine = def.engine();

        ValidationResult vr = engine.validate(state, command);
        if (!vr.isOk()) {
            return new CommandResult<>(false, state, List.of(), vr);
        }
        List<E> events = engine.decide(state, command);
        S newState = state;
        for (E e : events) {
            newState = engine.apply(newState, e);
        }
        entity.setState(serializer.serialize(newState));
        repo.save(entity);
        return new CommandResult<>(true, newState, events, vr);
    }

    @SuppressWarnings("unchecked")
    private <S, C extends Command, E extends Event> GameDefinition<S, C, E, ?> cast(GameDefinition<?, ?, ?, ?> raw) {
        return (GameDefinition<S, C, E, ?>) raw;
    }
}
