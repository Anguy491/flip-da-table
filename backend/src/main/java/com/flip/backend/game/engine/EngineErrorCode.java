package com.flip.backend.game.engine;

/** Canonical error codes for validation failures. */
public enum EngineErrorCode {
    ILLEGAL_TURN,
    INVALID_COMMAND,
    RULE_VIOLATION,
    OUT_OF_RANGE,
    DUPLICATE_ACTION,
    GAME_TERMINATED
}