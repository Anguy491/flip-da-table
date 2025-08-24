package com.flip.backend.game.uno.command;

import com.flip.backend.game.uno.UnoColor;
import com.flip.backend.game.uno.UnoValue;

/** Play a card the player owns (for wild choose-color occurs separately). */
public record PlayCard(String playerId, UnoColor color, UnoValue value) implements UnoCommand { }
