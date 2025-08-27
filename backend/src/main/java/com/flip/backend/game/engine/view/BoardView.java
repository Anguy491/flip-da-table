package com.flip.backend.game.engine.view;

/** Generic immutable board view DTO. */
public record BoardView(
	String gameType,
	long turnCount,
	int direction,
	int currentPlayerIndex
) {}
