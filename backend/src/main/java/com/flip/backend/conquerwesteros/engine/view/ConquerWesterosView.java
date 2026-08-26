package com.flip.backend.conquerwesteros.engine.view;

import java.time.Instant;
import java.util.List;

/** Perspective-safe transport records for the Conquer Westeros table. */
public final class ConquerWesterosView {
    private ConquerWesterosView() {}

    public record GameView(
            int schemaVersion,
            String phase,
            long stateVersion,
            String campaign,
            String campaignName,
            long turnCount,
            String viewerId,
            String currentPlayerId,
            String ironThroneHolderId,
            List<DieView> currentRoll,
            AttemptView attempt,
            List<PlayerView> players,
            List<StrongholdView> strongholds,
            LegalActionsView legalActions,
            List<ActionLogEntry> events,
            List<ResultView> results
    ) {}

    public record DieView(int dieId, String face, int militaryStrength, String display) {}

    public record AttemptView(
            String targetId,
            String targetOwnerId,
            boolean stealing,
            List<String> completedLineIds,
            List<Integer> lostDieIds,
            List<Integer> committedDieIds,
            List<LineView> requiredLines
    ) {}

    public record PlayerView(
            String playerId,
            String name,
            int seatIndex,
            boolean current,
            boolean holdsThrone,
            List<String> faceUpStrongholds,
            List<ClanView> completedClans,
            int strongholdCount,
            int completedClanCount,
            int faceUpScore,
            int clanScore,
            int totalScore
    ) {}

    public record ClanView(String name, int score, List<String> strongholdIds) {}

    public record StrongholdView(
            String id,
            String name,
            String clan,
            int points,
            boolean kingsLanding,
            String ownerId,
            boolean central,
            boolean locked,
            boolean stealCrownRequired,
            List<LineView> lines
    ) {}

    public record LineView(
            String id,
            String type,
            Integer threshold,
            List<String> symbols,
            String display,
            boolean completed,
            boolean special
    ) {}

    public record LegalActionsView(
            boolean canRoll,
            boolean canCompleteLine,
            boolean canLoseDie,
            List<String> legalTargetIds,
            List<Integer> legalDieIds
    ) {}

    public record ActionLogEntry(
            long sequence,
            String type,
            String actorId,
            String targetId,
            String text,
            Instant occurredAt
    ) {}

    public record PublicEvent(
            long sequence,
            String type,
            String actorId,
            String targetId,
            String lineId,
            Integer dieId,
            List<Integer> dieIds,
            String text,
            Instant occurredAt
    ) {}

    public record ResultView(
            String playerId,
            String name,
            int rank,
            int totalScore,
            int faceUpScore,
            int clanScore,
            int thronePoint,
            int strongholdCount,
            int completedClanCount,
            boolean winner
    ) {}
}
