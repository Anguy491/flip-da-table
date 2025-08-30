package com.flip.backend.api;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Manages per-game SSE emitters for UNO view updates. */
@Component
public class UnoSseService {
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String gameId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout (rely on client)
        emitters.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(gameId, emitter));
        emitter.onTimeout(() -> remove(gameId, emitter));
        emitter.onError(e -> remove(gameId, emitter));
        // Initial ping
        try { emitter.send(SseEmitter.event().name("INIT").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    private void remove(String gameId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(gameId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) emitters.remove(gameId);
        }
    }

    public void broadcastView(String gameId, Map<String,Object> viewPayload) {
        Set<SseEmitter> set = emitters.get(gameId);
        if (set == null || set.isEmpty()) return;
        // Copy to avoid CME
        for (SseEmitter em : Set.copyOf(set)) {
            try {
                em.send(SseEmitter.event().name("VIEW").data(viewPayload));
            } catch (IOException e) {
                remove(gameId, em);
            }
        }
    }
}
