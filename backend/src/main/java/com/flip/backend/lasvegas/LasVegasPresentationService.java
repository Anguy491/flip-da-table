package com.flip.backend.lasvegas;

import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Ephemeral, non-rule visibility state. It is intentionally absent from snapshots and versions. */
@Service
public class LasVegasPresentationService {
    private final Map<String, Set<String>> visiblePlayers = new ConcurrentHashMap<>();
    private final AtomicLong presentationSequence = new AtomicLong();

    public PublicEvent setVisible(String gameId, String playerId, boolean visible, LasVegasRuntimePhase runtime) {
        if (runtime.state() == LasVegasRuntimePhase.State.FINISHED) {
            clear(gameId);
            throw new IllegalArgumentException("asset presentation is unavailable after the game ends");
        }
        Set<String> visibleForGame = visiblePlayers.computeIfAbsent(gameId, ignored -> ConcurrentHashMap.newKeySet());
        if (visible) visibleForGame.add(playerId);
        else visibleForGame.remove(playerId);
        if (visibleForGame.isEmpty()) visiblePlayers.remove(gameId, visibleForGame);
        Integer total = visible ? runtime.totalAssets(playerId) : null;
        return new PublicEvent(
                presentationSequence.incrementAndGet(),
                "ASSET_VISIBILITY",
                playerId,
                null,
                null,
                null,
                null,
                total,
                visible,
                visible ? "A player revealed their total assets" : "A player hid their total assets",
                Instant.now()
        );
    }

    public Map<String, Integer> totals(String gameId, LasVegasRuntimePhase runtime) {
        return runtime.totalsFor(visiblePlayers.getOrDefault(gameId, Set.of()));
    }

    public void clear(String gameId) {
        visiblePlayers.remove(gameId);
    }
}
