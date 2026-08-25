package com.flip.backend.lasvegas.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.entities.LasVegasPlayer;

public final class AdvanceRoundEvent extends GameEvent {
    private final LasVegasRuntimePhase runtime;

    public AdvanceRoundEvent(LasVegasRuntimePhase runtime, LasVegasPlayer source) {
        super(source, System.currentTimeMillis());
        this.runtime = runtime;
    }

    @Override public boolean isValid() { return runtime.canAdvanceRound(); }
    @Override public void execute() { runtime.advanceRound(); }
}
