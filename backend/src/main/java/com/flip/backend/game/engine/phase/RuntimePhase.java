package com.flip.backend.game.engine.phase;

public abstract class RuntimePhase extends Phase {
    /** Execute loop. Must call enter() before run if initialization needed. */
    public abstract String run();
}
