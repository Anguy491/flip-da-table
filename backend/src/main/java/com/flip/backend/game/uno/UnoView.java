package com.flip.backend.game.uno;

import java.util.List;

/** Projected view for a single viewer. */
public record UnoView(String viewerId, List<UnoPlayerView> players, int pendingDraw, boolean mustChooseColor, UnoCard top, GamePhase phase) { }
