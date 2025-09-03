package com.flip.backend.dvc.engine.view;

import java.util.List;

/** Per-player private view: full own hand values & reveal flags; opponents only get counts & reveal statuses. */
public record DVCPlayerView(
    String playerId,
    boolean bot,
    int handSize,
    int hiddenCount,
    List<String> cards,        // for self: each element either actual (e.g. BLACK 3, WHITE -, etc.) or ? for hidden opponents if we choose unify; here we supply only for self
    List<Boolean> revealedFlags // parallel to cards/hand ordering (full for self, for opponents only reveal pattern)
) {}
