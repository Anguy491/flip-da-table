package com.flip.backend.uno.engine;

import com.flip.backend.uno.engine.phase.UnoRuntimePhase;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** In-memory UNO runtime store (prototype). */
@Component
public class UnoGameRegistry {
    private final Map<String, UnoRuntimePhase> runtimes = new ConcurrentHashMap<>();

    public void put(String gameId, UnoRuntimePhase runtime) { runtimes.put(gameId, runtime); }
    public UnoRuntimePhase get(String gameId) { return runtimes.get(gameId); }
    public boolean exists(String gameId) { return runtimes.containsKey(gameId); }
}
