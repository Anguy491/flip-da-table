package com.flip.backend.game.uno.event;

import com.flip.backend.game.uno.UnoColor;
import com.flip.backend.game.uno.UnoValue;

public record CardPlayed(String playerId, UnoColor color, UnoValue value) implements UnoEvent { }
