package com.flip.backend.game.engine.event;

import com.flip.backend.game.entities.Player;

public abstract class GameEvent {
    protected Player source;
    protected long timestamp;

}
