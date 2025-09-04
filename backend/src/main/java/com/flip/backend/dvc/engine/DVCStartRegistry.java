package com.flip.backend.dvc.engine;

import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** Registry for DVC start phases waiting for all players to settle initial hands. */
@Component
public class DVCStartRegistry {
    private final Map<String, DVCStartPhase> starts = new ConcurrentHashMap<>();
    public void put(String gameId, DVCStartPhase start) { starts.put(gameId, start); }
    public DVCStartPhase get(String gameId) { return starts.get(gameId); }
    public void remove(String gameId) { starts.remove(gameId); }
    public boolean exists(String gameId) { return starts.containsKey(gameId); }
}