package com.flip.backend.game.engine.event;

import com.flip.backend.game.engine.GameState;
import com.flip.backend.game.entities.Player;

public abstract class GameEvent {
    protected Player source;
    protected long timestamp;

    public abstract void execute(GameState state);
    public abstract boolean isValid(GameState state);
}
