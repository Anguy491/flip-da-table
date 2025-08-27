package com.flip.backend.game.engine.event;

import com.flip.backend.game.entities.Player;

public abstract class GameEvent {
    protected final Player source;
    protected final long timestamp;

    protected GameEvent(Player source, long timestamp) {
        this.source = source; this.timestamp = timestamp;
    }

    public Player source() { return source; }
    public long timestamp() { return timestamp; }

    public abstract boolean isValid();
    public abstract void execute();
}
