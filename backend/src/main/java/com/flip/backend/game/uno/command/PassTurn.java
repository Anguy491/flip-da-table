package com.flip.backend.game.uno.command;

public record PassTurn(String playerId) implements UnoCommand { }
