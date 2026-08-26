package com.flip.backend.conquerwesteros.engine.phase;

import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ResultView;
import com.flip.backend.game.engine.phase.EndingPhase;

import java.util.List;

public final class ConquerWesterosEndingPhase extends EndingPhase {
    private final List<ResultView> results;

    public ConquerWesterosEndingPhase(List<ResultView> results) {
        this.results = List.copyOf(results);
    }

    public List<ResultView> results() { return results; }
}
