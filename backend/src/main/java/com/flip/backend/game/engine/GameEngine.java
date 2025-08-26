package com.flip.backend.game.engine;

import com.flip.backend.game.engine.phase.*;

/** Simple engine that starts with a StartPhase and auto-transits to RuntimePhase. */
public class GameEngine {
    private Phase current;
    private boolean started = false;

    public GameEngine(StartPhase startPhase) { this.current = startPhase; }

    public Phase currentPhase() { return current; }

    public void start() {
        if (started) throw new IllegalStateException("Engine already started");
        started = true;
        current.enter();
        if (current instanceof StartPhase sp) {
            current = sp.transit();
        }
    }

    public String run() {
        if (!started) throw new IllegalStateException("Engine not started");
        if (current instanceof RuntimePhase rp) {
            rp.enter(); // idempotent / no-op in current impl
            return rp.run();
        }
        return null; // not a runtime phase
    }
}
