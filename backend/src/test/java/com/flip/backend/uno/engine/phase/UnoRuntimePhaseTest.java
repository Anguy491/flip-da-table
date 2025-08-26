package com.flip.backend.uno.engine.phase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class UnoRuntimePhaseTest {
    @Test
    void simpleLoopProducesWinner() {
        UnoStartPhase start = new UnoStartPhase(List.of("P1","P2","P3"));
        start.enter();
        var runtime = start.transit();
        String winner = runtime.run();
        assertNotNull(winner);
        assertTrue(List.of("P1","P2","P3").contains(winner));
    }
}
