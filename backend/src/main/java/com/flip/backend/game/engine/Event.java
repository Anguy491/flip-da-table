package com.flip.backend.game.engine;

/** Marker interface for domain events (facts). */
public interface Event {
    /** Monotonic sequence within a room, filled by the room/actor, not the engine. */
    default long sequence() { return -1L; }
}