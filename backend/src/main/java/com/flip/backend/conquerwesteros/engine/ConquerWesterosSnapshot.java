package com.flip.backend.conquerwesteros.engine;

import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ActionLogEntry;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.ResultView;

import java.util.List;
import java.util.Map;

/** Complete aggregate snapshot persisted in games.state_json. */
public record ConquerWesterosSnapshot(
        int schemaVersion,
        String campaign,
        String phase,
        long turnCount,
        long stateVersion,
        long eventSequence,
        String currentPlayerId,
        String ironThroneHolderId,
        List<String> centralStrongholds,
        List<PlayerState> players,
        AttemptState attempt,
        List<RollState> currentRoll,
        List<ActionLogEntry> actionLog,
        List<ResultView> results
) {
    public record PlayerState(
            String playerId,
            String name,
            boolean bot,
            List<String> faceUpStrongholds,
            Map<String, List<String>> completedClans
    ) {
        /** Source-compatible helper for v1 human-only fixtures and migrations. */
        public PlayerState(
                String playerId,
                String name,
                List<String> faceUpStrongholds,
                Map<String, List<String>> completedClans
        ) {
            this(playerId, name, false, faceUpStrongholds, completedClans);
        }
    }

    public record AttemptState(
            String targetId,
            String targetOwnerId,
            boolean stealing,
            Map<String, List<Integer>> committedLines,
            List<Integer> lostDieIds
    ) {}

    public record RollState(int dieId, String face) {}
}
