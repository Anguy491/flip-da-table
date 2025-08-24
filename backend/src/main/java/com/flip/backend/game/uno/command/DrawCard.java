package com.flip.backend.game.uno.command;

public record DrawCard(String playerId) implements com.flip.backend.game.uno.command.UnoCommand { }
