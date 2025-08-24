package com.flip.backend.game.uno.event;

public record CardDrawn(String playerId, int count) implements UnoEvent { }
