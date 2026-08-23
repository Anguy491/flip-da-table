package com.flip.backend.api;

import com.flip.backend.security.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Manages per-game SSE emitters for UNO view updates. */
@Component
public class UnoSseService {
    private final long timeoutMs;
    private final int maxPerPlayer;
    private final int maxPerGame;
    private final Map<String, Set<Subscription>> subscriptions = new ConcurrentHashMap<>();

    public UnoSseService(
            @Value("${app.realtime.sse.timeout-ms:1800000}") long timeoutMs,
            @Value("${app.realtime.sse.max-per-player:3}") int maxPerPlayer,
            @Value("${app.realtime.sse.max-per-game:32}") int maxPerGame
    ) {
        if (timeoutMs < 1 || maxPerPlayer < 1 || maxPerGame < maxPerPlayer) {
            throw new IllegalArgumentException("invalid SSE limit configuration");
        }
        this.timeoutMs = timeoutMs;
        this.maxPerPlayer = maxPerPlayer;
        this.maxPerGame = maxPerGame;
    }

    public SseEmitter subscribe(String gameId, String playerId, Supplier<Map<String,Object>> viewSupplier) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        Subscription subscription = new Subscription(playerId, emitter, viewSupplier);
        reserve(gameId, subscription);
        emitter.onCompletion(() -> remove(gameId, subscription));
        emitter.onTimeout(() -> remove(gameId, subscription));
        emitter.onError(error -> remove(gameId, subscription));
        try {
            emitter.send(SseEmitter.event().name("INIT").data("ok"));
            emitter.send(SseEmitter.event().name("VIEW").data(viewSupplier.get()));
        } catch (IOException | RuntimeException ex) {
            remove(gameId, subscription);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    private synchronized void reserve(String gameId, Subscription subscription) {
        Set<Subscription> set = subscriptions.computeIfAbsent(gameId, key -> ConcurrentHashMap.newKeySet());
        if (set.size() >= maxPerGame) {
            throw new RateLimitExceededException("too many game streams", timeoutMs / 1000L);
        }
        long playerStreams = set.stream().filter(existing -> existing.playerId().equals(subscription.playerId())).count();
        if (playerStreams >= maxPerPlayer) {
            throw new RateLimitExceededException("too many player streams", timeoutMs / 1000L);
        }
        set.add(subscription);
    }

    private synchronized void remove(String gameId, Subscription subscription) {
        Set<Subscription> set = subscriptions.get(gameId);
        if (set != null) {
            set.remove(subscription);
            if (set.isEmpty()) subscriptions.remove(gameId);
        }
    }

    public void broadcastView(String gameId) {
        Set<Subscription> snapshot = snapshot(gameId);
        for (Subscription subscription : snapshot) {
            try {
                subscription.emitter().send(SseEmitter.event().name("VIEW").data(subscription.viewSupplier().get()));
            } catch (IOException | RuntimeException ex) {
                remove(gameId, subscription);
            }
        }
    }

    private synchronized Set<Subscription> snapshot(String gameId) {
        Set<Subscription> set = subscriptions.get(gameId);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    private record Subscription(String playerId, SseEmitter emitter, Supplier<Map<String,Object>> viewSupplier) {}
}
