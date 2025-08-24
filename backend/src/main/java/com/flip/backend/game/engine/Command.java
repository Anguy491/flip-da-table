package com.flip.backend.game.engine;

/** Marker interface for game commands (intent). */
public interface Command {
    /** Client-supplied idempotency key for de-duplication, optional but recommended. */
    default String commandId() { return null; }
}