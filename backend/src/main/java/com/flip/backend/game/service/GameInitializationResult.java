package com.flip.backend.game.service;

import com.flip.backend.api.dto.LobbyDtos.PlayerInfo;
import java.util.List;

/** Outcome from initializing a game: lifecycle state + optional serialized state + player mapping. */
public record GameInitializationResult(
        String lifecycleState,
        String stateJson,
        List<PlayerInfo> players
) {}
