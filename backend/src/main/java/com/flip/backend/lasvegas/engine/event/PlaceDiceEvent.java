package com.flip.backend.lasvegas.engine.event;

import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.entities.LasVegasPlayer;

public final class PlaceDiceEvent extends GameEvent {
    private final LasVegasRuntimePhase runtime;
    private final LasVegasPlayer player;
    private final int face;
    private final EventQueue queue;

    public PlaceDiceEvent(LasVegasRuntimePhase runtime, LasVegasPlayer player, int face, EventQueue queue) {
        super(player, System.currentTimeMillis());
        this.runtime = runtime;
        this.player = player;
        this.face = face;
        this.queue = queue;
    }

    @Override public boolean isValid() { return runtime.canPlace(player, face); }
    @Override public void execute() { runtime.placeDice(player, face, queue); }
}
