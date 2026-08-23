package com.flip.backend.security;

public class GameStateConflictException extends RuntimeException {
    public GameStateConflictException(String message) {
        super(message);
    }
}
