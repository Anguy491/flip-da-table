package com.flip.backend.lasvegas.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.entities.LasVegasPlayer;

public final class RollDiceEvent extends GameEvent {
    private final LasVegasRuntimePhase runtime;
    private final LasVegasPlayer player;

    public RollDiceEvent(LasVegasRuntimePhase runtime, LasVegasPlayer player) {
        super(player, System.currentTimeMillis());
        this.runtime = runtime;
        this.player = player;
    }

    @Override public boolean isValid() { return runtime.canRoll(player); }
    @Override public void execute() { runtime.rollDice(player); }
}
