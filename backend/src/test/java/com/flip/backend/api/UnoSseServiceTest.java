package com.flip.backend.api;

import com.flip.backend.security.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnoSseServiceTest {
    @Test
    void capsStreamsPerPlayerAndPerGame() {
        var service = new UnoSseService(60_000L, 2, 3);
        assertDoesNotThrow(() -> service.subscribe("game-1", "player-1", Map::of));
        assertDoesNotThrow(() -> service.subscribe("game-1", "player-1", Map::of));
        assertThrows(RateLimitExceededException.class,
                () -> service.subscribe("game-1", "player-1", Map::of));

        assertDoesNotThrow(() -> service.subscribe("game-1", "player-2", Map::of));
        assertThrows(RateLimitExceededException.class,
                () -> service.subscribe("game-1", "player-3", Map::of));
    }
}
