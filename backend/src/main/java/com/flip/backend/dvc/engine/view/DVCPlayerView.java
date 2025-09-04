package com.flip.backend.dvc.engine.view;

import java.util.List;

/** DVC player view: cards list already context-filtered (self: front/back mixed; opponent: front for revealed, back for hidden). */
public record DVCPlayerView(
    String playerId,
    boolean bot,
    int handSize,
    int hiddenCount,
    List<String> cards
) {}
