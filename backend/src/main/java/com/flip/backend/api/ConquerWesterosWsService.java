package com.flip.backend.api;

import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PublicEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConquerWesterosWsService {
    private static final Logger log = LoggerFactory.getLogger(ConquerWesterosWsService.class);
    private final SimpMessagingTemplate messaging;
    private final Counter privateViewFailures;
    private final Counter publicEventFailures;

    public ConquerWesterosWsService(SimpMessagingTemplate messaging, MeterRegistry meters) {
        this.messaging = messaging;
        this.privateViewFailures = Counter.builder("conquerwesteros.websocket.broadcast.failures")
                .tag("channel", "private-view").register(meters);
        this.publicEventFailures = Counter.builder("conquerwesteros.websocket.broadcast.failures")
                .tag("channel", "public-event").register(meters);
    }

    public void broadcastViews(String gameId, Map<String, GameView> views) {
        views.forEach((playerId, view) -> {
            try {
                messaging.convertAndSend("/topic/conquer-westeros/" + gameId + "/" + playerId, view);
            } catch (RuntimeException exception) {
                privateViewFailures.increment();
                log.error("Conquer Westeros private view broadcast failed for game {} player {}", gameId, playerId, exception);
            }
        });
    }

    public void broadcastEvents(String gameId, List<PublicEvent> events) {
        if (events == null || events.isEmpty()) return;
        try {
            messaging.convertAndSend("/topic/conquer-westeros/" + gameId + "/events", events);
        } catch (RuntimeException exception) {
            publicEventFailures.increment();
            log.error("Conquer Westeros public event broadcast failed for game {}", gameId, exception);
        }
    }
}
