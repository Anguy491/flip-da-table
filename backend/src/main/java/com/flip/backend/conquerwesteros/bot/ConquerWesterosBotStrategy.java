package com.flip.backend.conquerwesteros.bot;

import java.util.List;

/** Chooses one legal Conquer Westeros action using public table information only. */
public interface ConquerWesterosBotStrategy {
    Decision choose(TurnState state);

    record Decision(
            String type,
            String targetId,
            String lineId,
            List<Integer> dieIds,
            Integer dieId
    ) {
        public Decision {
            dieIds = dieIds == null ? List.of() : List.copyOf(dieIds);
        }

        public static Decision completeLine(String targetId, String lineId, List<Integer> dieIds) {
            return new Decision("COMPLETE_LINE", targetId, lineId, dieIds, null);
        }

        public static Decision loseDie(int dieId) {
            return new Decision("LOSE_DIE", null, null, List.of(), dieId);
        }
    }

    record TurnState(
            String botId,
            String ironThroneHolderId,
            boolean targetLocked,
            List<DieState> currentRoll,
            List<TargetState> targets
    ) {
        public TurnState {
            currentRoll = List.copyOf(currentRoll);
            targets = List.copyOf(targets);
        }
    }

    record DieState(int dieId, String face, int militaryStrength) {}

    record TargetState(
            String id,
            int points,
            int clanScore,
            int clanSize,
            int botOwnedClanCount,
            int botOwnedClanFaceUpPoints,
            boolean kingsLanding,
            String ownerId,
            boolean central,
            List<LineState> remainingLines
    ) {
        public TargetState {
            remainingLines = List.copyOf(remainingLines);
        }
    }

    record LineState(
            String id,
            String type,
            Integer threshold,
            List<String> symbols
    ) {
        public LineState {
            symbols = symbols == null ? List.of() : List.copyOf(symbols);
        }
    }
}
