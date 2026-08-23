package com.flip.backend.dvc.engine;

import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DVCRuntimeConcurrencyTest {
    @Test
    void concurrentDuplicateCommandsCanAdvanceTheStateOnlyOnce() throws Exception {
        var start = new DVCStartPhase(List.of("A", "B"));
        start.enter();
        start.settled("A");
        start.settled("B");
        var runtime = start.transit();
        runtime.enter();
        int cardsBefore = runtime.deck().remaining();

        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown();
                go.await();
                return runtime.provideDrawColor("A", "BLACK");
            });
            var second = executor.submit(() -> {
                ready.countDown();
                go.await();
                return runtime.provideDrawColor("A", "BLACK");
            });
            ready.await();
            go.countDown();

            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successes);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(cardsBefore - 1, runtime.deck().remaining());
        assertTrue(runtime.board().hasPending("A"));
    }
}
