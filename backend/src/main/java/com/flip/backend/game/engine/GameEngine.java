// src/main/java/com/flip/backend/game/engine/GameEngine.java
package com.flip.backend.game.engine;

import java.util.List;

/**
 * Pure-functional game engine abstraction.
 * S - immutable state, C - command, E - event.
 */
public interface GameEngine<S, C extends Command, E extends Event> {

    /** Validate whether the command can be applied on the given state. */
    ValidationResult validate(S state, C command);

    /**
     * Decide events for a validated command.
     * Precondition: validate(state, command).isOk() == true
     * Postcondition: events are deterministic for same (state, command).
     */
    List<E> decide(S state, C command);

    /** Apply a single event to state and return the next state. */
    S apply(S state, E event);

    /** Optional: create a compact snapshot for quick recovery. */
    default Snapshot<S> snapshot(S state, long atSequence, int version) {
        return Snapshot.of(state, atSequence, version);
    }

    /** Restore state from snapshot and tail events. */
    default S restore(Snapshot<S> snap, List<E> tail) {
        S s = snap.state();
        if (tail != null) {
            for (E e : tail) {
                s = apply(s, e);
            }
        }
        return s;
    }

    /** Whether the game has reached terminal status. */
    boolean isTerminal(S state);
}
