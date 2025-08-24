package com.flip.backend.game.uno.event;

public record PlayerSkipped(String skippedPlayerId) implements UnoEvent { }
