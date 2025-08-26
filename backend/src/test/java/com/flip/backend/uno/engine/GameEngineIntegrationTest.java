package com.flip.backend.uno.engine;

import com.flip.backend.game.engine.GameEngine;
import com.flip.backend.game.engine.phase.Phase;
import com.flip.backend.uno.engine.phase.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class GameEngineIntegrationTest {

    @Test
    void engineAutoTransitionsAndRuns() {
        UnoStartPhase start = new UnoStartPhase(List.of("A","B","C"));
        GameEngine engine = new GameEngine(start);
        engine.start();
        Phase p = engine.currentPhase();
        assertTrue(p instanceof UnoRuntimePhase, "Should transition to runtime phase");
        String winner = engine.run();
        assertNotNull(winner);
        assertTrue(List.of("A","B","C").contains(winner));
    }

    @Test
    void doubleStartThrows() {
        GameEngine engine = new GameEngine(new UnoStartPhase(List.of("P1","P2")));
        engine.start();
        assertThrows(IllegalStateException.class, engine::start);
    }

    @Test
    void runBeforeStartThrows() {
        GameEngine engine = new GameEngine(new UnoStartPhase(List.of("P1","P2")));
        assertThrows(IllegalStateException.class, engine::run);
    }
}
