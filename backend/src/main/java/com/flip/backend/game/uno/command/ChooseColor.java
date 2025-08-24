package com.flip.backend.game.uno.command;

import com.flip.backend.game.uno.UnoColor;

public record ChooseColor(String playerId, UnoColor color) implements UnoCommand { }
