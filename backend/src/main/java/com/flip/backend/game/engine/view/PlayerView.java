package com.flip.backend.game.engine.view;

/** Generic player view, exposing id and whether it's a bot. */
public record PlayerView(
	String playerId,
	boolean bot
) {}
