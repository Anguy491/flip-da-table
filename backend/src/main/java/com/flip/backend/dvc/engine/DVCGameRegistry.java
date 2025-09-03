package com.flip.backend.dvc.engine;

import com.flip.backend.dvc.engine.phase.DVCRuntimePhase;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** In-memory DVC runtime registry (prototype scope). */
@Component
public class DVCGameRegistry {
	private final Map<String, DVCRuntimePhase> runtimes = new ConcurrentHashMap<>();

	public void put(String gameId, DVCRuntimePhase runtime) { runtimes.put(gameId, runtime); }
	public DVCRuntimePhase get(String gameId) { return runtimes.get(gameId); }
	public boolean exists(String gameId) { return runtimes.containsKey(gameId); }
}
