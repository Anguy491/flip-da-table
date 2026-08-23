package com.flip.backend.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GamePlayerRegistry {
    private final Map<String, Map<Long, String>> playersByGame = new ConcurrentHashMap<>();

    public void put(String gameId, Map<Long, String> players) {
        playersByGame.put(gameId, Map.copyOf(players));
    }

    public String playerId(String gameId, Long userId) {
        Map<Long, String> players = playersByGame.get(gameId);
        return players == null ? null : players.get(userId);
    }
}
