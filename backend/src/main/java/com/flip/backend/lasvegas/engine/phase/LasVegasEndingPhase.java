package com.flip.backend.lasvegas.engine.phase;

import com.flip.backend.game.engine.phase.EndingPhase;
import com.flip.backend.lasvegas.engine.view.LasVegasView.ResultView;

import java.util.List;

public final class LasVegasEndingPhase extends EndingPhase {
    private final List<ResultView> results;

    public LasVegasEndingPhase(List<ResultView> results) {
        this.results = List.copyOf(results);
    }

    public List<ResultView> results() {
        return results;
    }
}
