package com.flip.backend.uno.engine.view;

/** UNO board view snapshot. */
public record UnoBoardView(
	String gameType,
	long turnCount,
	int direction,
	int currentPlayerIndex,
	String topCard,
	String activeColor,
	int drawPileSize,
	int discardPileSize
) {}
