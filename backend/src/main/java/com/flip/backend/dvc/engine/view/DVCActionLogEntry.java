package com.flip.backend.dvc.engine.view;

/** Public, perspective-safe DVC action history entry. */
public record DVCActionLogEntry(
    long seq,
    long turnId,
    String type,
    String actorId,
    String targetPlayerId,
    Integer targetPosition,
    String guessValue,
    Boolean correct,
    String text,
    long timestamp
) {}
