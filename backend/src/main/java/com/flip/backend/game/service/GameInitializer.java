package com.flip.backend.game.service;

import com.flip.backend.game.engine.GameType;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.persistence.SessionEntity;

/** Strategy to initialize a game for a session (first round). */
public interface GameInitializer {
    GameType supports();
    GameInitializationResult initialize(SessionEntity session, StartGameRequest request);
}
