package com.flip.backend.game.service;

import com.flip.backend.game.engine.Event;
import com.flip.backend.game.engine.ValidationResult;
import java.util.List;

/** Outcome of handling a command: new state (if ok) + events + validation. */
public record CommandResult<S, E extends Event>(
        boolean applied,
        S state,
        List<E> events,
        ValidationResult validation
) {}
