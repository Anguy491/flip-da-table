package com.flip.backend.game.service;

import com.flip.backend.game.engine.GameType;

public interface GameRuntimeDelegate {
    GameType supports();
}
