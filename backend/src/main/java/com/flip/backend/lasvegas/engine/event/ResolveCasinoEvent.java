package com.flip.backend.lasvegas.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.entities.LasVegasPlayer;

public final class ResolveCasinoEvent extends GameEvent {
    private final LasVegasRuntimePhase runtime;
    private final int casinoNumber;

    public ResolveCasinoEvent(LasVegasRuntimePhase runtime, LasVegasPlayer source, int casinoNumber) {
        super(source, System.currentTimeMillis());
        this.runtime = runtime;
        this.casinoNumber = casinoNumber;
    }

    @Override public boolean isValid() { return runtime.canResolveCasino(casinoNumber); }
    @Override public void execute() { runtime.resolveCasino(casinoNumber); }
}
