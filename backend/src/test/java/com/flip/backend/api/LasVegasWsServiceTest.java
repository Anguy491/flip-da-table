package com.flip.backend.api;

import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class LasVegasWsServiceTest {
    @Test
    void countsPrivateAndPublicBroadcastFailuresWithoutBreakingTheCommittedCommand() {
        var messaging = mock(SimpMessagingTemplate.class);
        var meters = new SimpleMeterRegistry();
        var service = new LasVegasWsService(messaging, meters);
        var view = mock(GameView.class);
        var event = mock(PublicEvent.class);

        doThrow(new IllegalStateException("private channel down"))
                .when(messaging).convertAndSend(eq("/topic/las-vegas/G1/P1"), any(Object.class));
        doThrow(new IllegalStateException("public channel down"))
                .when(messaging).convertAndSend(eq("/topic/las-vegas/G1/events"), any(Object.class));

        service.broadcastViews("G1", Map.of("P1", view));
        service.broadcastEvents("G1", List.of(event));

        assertEquals(1.0, meters.get("lasvegas.websocket.broadcast.failures")
                .tag("channel", "private-view").counter().count());
        assertEquals(1.0, meters.get("lasvegas.websocket.broadcast.failures")
                .tag("channel", "public-event").counter().count());
    }
}
