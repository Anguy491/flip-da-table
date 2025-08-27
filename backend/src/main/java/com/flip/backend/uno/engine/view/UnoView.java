package com.flip.backend.uno.engine.view;

import java.util.List;

/** Aggregated UNO view for a requesting player. */
public record UnoView(
	UnoBoardView board,
	List<UnoPlayerView> players,
	String perspectivePlayerId
) {}
