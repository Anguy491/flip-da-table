package com.flip.backend.game.uno.event;

import com.flip.backend.game.uno.UnoColor;

public record ColorChosen(String playerId, UnoColor color) implements UnoEvent { }
