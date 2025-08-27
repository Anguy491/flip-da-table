package com.flip.backend.uno.engine.view;

/** View of a single UNO player from perspective: either full hand or only count. */
public record UnoPlayerView(
	String playerId,
	boolean bot,
	int handSize,
	java.util.List<String> hand // may be null or empty for opponents
) {}
