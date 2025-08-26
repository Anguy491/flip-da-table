package com.flip.backend.game.entities;

/**
 * Base card abstraction. Concrete games can extend this to add
 * game specific attributes (color, rank, etc.).
 */
public abstract class Card {
	/**
	 * A human readable label for debug / logging.
	 */
	public abstract String getDisplay();
}
