package com.flip.backend.dvc.engine.view;

import java.util.List;

/** Aggregate DVC view delivered to a single client via websocket. */
public record DVCView(
    DVCBoardView board,
    List<DVCPlayerView> players,
    String perspectivePlayerId
) {}
