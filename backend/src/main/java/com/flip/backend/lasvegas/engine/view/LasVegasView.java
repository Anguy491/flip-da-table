package com.flip.backend.lasvegas.engine.view;

import java.time.Instant;
import java.util.List;

/** Perspective-safe transport records for the Las Vegas table. */
public final class LasVegasView {
    private LasVegasView() {}

    public record GameView(
            int schemaVersion,
            String phase,
            long stateVersion,
            int internalRound,
            int totalRounds,
            long turnCount,
            String viewerId,
            String currentPlayerId,
            List<RollView> currentRoll,
            List<PlayerView> players,
            List<CasinoView> casinos,
            List<ActionLogEntry> events,
            List<ResultView> results
    ) {}

    public record RollView(int face, boolean big) {}

    public record PlayerView(
            String playerId,
            String name,
            boolean bot,
            int seatIndex,
            boolean current,
            int remainingRegularDice,
            boolean bigDieRemaining,
            int remainingDice,
            int chips,
            int moneyCardCount,
            List<Integer> moneyCards,
            Integer cashTotal,
            Integer totalAssets,
            Integer presentedTotal
    ) {}

    public record CasinoView(
            int number,
            List<Integer> bonuses,
            List<PlacementView> placements
    ) {}

    public record PlacementView(
            String playerId,
            int regularDice,
            boolean bigDie,
            int influence
    ) {}

    public record ActionLogEntry(
            long sequence,
            String type,
            String actorId,
            Integer casinoNumber,
            String text,
            Instant occurredAt
    ) {}

    /** Ephemeral public animation/presentation event; exact prize amounts are never snapshotted. */
    public record PublicEvent(
            long sequence,
            String type,
            String actorId,
            Integer casinoNumber,
            Integer face,
            Integer regularDice,
            Boolean bigDie,
            Integer amount,
            Boolean visible,
            String text,
            Instant occurredAt
    ) {}

    public record ResultView(
            String playerId,
            String name,
            int rank,
            int cashTotal,
            int chips,
            int totalAssets,
            int tieBreakCount,
            boolean winner
    ) {}
}
