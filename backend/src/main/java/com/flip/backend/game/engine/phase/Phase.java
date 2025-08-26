package com.flip.backend.game.engine.phase;

/** Base class for a game phase. */
public abstract class Phase {
	/** Perform phase entry logic (side-effects on game state). */
	public abstract void enter();
}
