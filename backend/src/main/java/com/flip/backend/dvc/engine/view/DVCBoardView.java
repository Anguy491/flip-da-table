package com.flip.backend.dvc.engine.view;

/** Public board snapshot for DVC. */
public record DVCBoardView(
    String gameType,
    long turnId,
    int direction,
    int currentPlayerIndex,
    int deckRemaining,
    String awaiting,          // current awaited input type (for current player only meaningful)
    String winnerId,
    int deckBlackRemaining,
    int deckWhiteRemaining
) {}

