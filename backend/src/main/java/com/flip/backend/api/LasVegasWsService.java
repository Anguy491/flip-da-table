package com.flip.backend.api;

import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LasVegasWsService {
    private static final Logger log = LoggerFactory.getLogger(LasVegasWsService.class);
    private final SimpMessagingTemplate messaging;
    private final Counter privateViewFailures;
    private final Counter publicEventFailures;

    public LasVegasWsService(SimpMessagingTemplate messaging, MeterRegistry meterRegistry) {
        this.messaging = messaging;
        this.privateViewFailures = Counter.builder("lasvegas.websocket.broadcast.failures")
                .tag("channel", "private-view")
                .description("Failed Las Vegas WebSocket broadcasts")
                .register(meterRegistry);
        this.publicEventFailures = Counter.builder("lasvegas.websocket.broadcast.failures")
                .tag("channel", "public-event")
                .description("Failed Las Vegas WebSocket broadcasts")
                .register(meterRegistry);
    }

    public void broadcastViews(String gameId, Map<String, GameView> views) {
        views.forEach((playerId, view) -> {
            try {
                messaging.convertAndSend("/topic/las-vegas/" + gameId + "/" + playerId, view);
            } catch (RuntimeException exception) {
                privateViewFailures.increment();
                log.error("Las Vegas private view broadcast failed for game {} player {}", gameId, playerId, exception);
            }
        });
    }

    public void broadcastEvents(String gameId, List<PublicEvent> events) {
        if (events == null || events.isEmpty()) return;
        try {
            messaging.convertAndSend("/topic/las-vegas/" + gameId + "/events", events);
        } catch (RuntimeException exception) {
            publicEventFailures.increment();
            log.error("Las Vegas public event broadcast failed for game {}", gameId, exception);
        }
    }
}
