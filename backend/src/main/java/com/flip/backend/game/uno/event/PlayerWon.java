package com.flip.backend.game.uno.event;

public record PlayerWon(String playerId) implements UnoEvent { }
