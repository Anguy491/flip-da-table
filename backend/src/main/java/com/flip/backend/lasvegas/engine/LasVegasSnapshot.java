package com.flip.backend.lasvegas.engine;

import com.flip.backend.lasvegas.engine.view.LasVegasView.ActionLogEntry;
import com.flip.backend.lasvegas.engine.view.LasVegasView.ResultView;

import java.util.List;

/** Complete aggregate snapshot persisted in games.state_json. */
public record LasVegasSnapshot(
        int schemaVersion,
        int internalRound,
        String phase,
        long turnCount,
        long stateVersion,
        long eventSequence,
        String currentPlayerId,
        String roundStarterId,
        List<PlayerState> players,
        List<Integer> deck,
        List<CasinoState> casinos,
        List<RollState> currentRoll,
        List<ActionLogEntry> actionLog,
        List<ResultView> results
) {
    public record PlayerState(
            String playerId,
            String name,
            boolean bot,
            int chips,
            int remainingRegularDice,
            boolean bigDieRemaining,
            List<Integer> moneyCards
    ) {}

    public record CasinoState(
            int number,
            int jackpot,
            int secondPrize,
            List<PlacementState> placements,
            String jackpotWinnerId
    ) {}

    public record PlacementState(String playerId, int regularDice, boolean bigDie) {}
    public record RollState(int face, boolean big) {}
}
