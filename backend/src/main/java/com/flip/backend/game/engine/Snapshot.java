// src/main/java/com/flip/backend/game/engine/Snapshot.java
package com.flip.backend.game.engine;

import java.util.Objects;

/** Immutable snapshot of a state at a specific event sequence and logical version. */
public final class Snapshot<S> {
    private final S state;
    private final long atSequence;
    private final int version;

    private Snapshot(S state, long atSequence, int version) {
        this.state = Objects.requireNonNull(state, "state");
        this.atSequence = atSequence;
        this.version = version;
    }

    public static <S> Snapshot<S> of(S state, long atSequence, int version) {
        return new Snapshot<>(state, atSequence, version);
    }

    public S state() { return state; }
    public long atSequence() { return atSequence; }
    public int version() { return version; }
}
