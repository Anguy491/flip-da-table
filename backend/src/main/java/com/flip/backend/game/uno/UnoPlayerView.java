package com.flip.backend.game.uno;

import java.util.List;

public record UnoPlayerView(String playerId, int handSize, List<UnoCard> hand, boolean isCurrent, boolean isWinner) { }
