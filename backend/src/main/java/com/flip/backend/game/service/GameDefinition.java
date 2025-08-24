package com.flip.backend.game.service;

import com.flip.backend.game.engine.GameEngine;
import com.flip.backend.game.engine.Command;
import com.flip.backend.game.engine.Event;
import com.flip.backend.game.engine.GameType;
import com.flip.backend.game.engine.StateProjector;

/** Bundles metadata needed to route a game id to its engine & state class. */
public record GameDefinition<S, C extends Command, E extends Event, V>(
        GameType type,
        Class<S> stateClass,
        GameEngine<S, C, E> engine,
        StateProjector<S, V> projector
) {}
