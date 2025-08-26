package com.flip.backend.game.engine.phase;

public abstract class StartPhase extends Phase {
	/** Transition to runtime phase after start initialization. */
	public abstract RuntimePhase transit();
}
