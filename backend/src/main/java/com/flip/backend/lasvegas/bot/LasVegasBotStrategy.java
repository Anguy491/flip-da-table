package com.flip.backend.lasvegas.bot;

import java.util.List;

/** Chooses one legal post-roll action from public table information only. */
public interface LasVegasBotStrategy {
    Decision choose(TurnState state);

    record Decision(String type, Integer face) {
        public static Decision place(int face) { return new Decision("PLACE_DICE", face); }
        public static Decision skip() { return new Decision("SKIP_TURN", null); }
    }

    record TurnState(
            String botId,
            int chips,
            List<DieState> currentRoll,
            List<CasinoState> casinos,
            List<PlayerState> players
    ) {}

    record DieState(int face, boolean big) {}
    record CasinoState(int number, List<Integer> bonuses, List<PlacementState> placements) {}
    record PlacementState(String playerId, int influence) {}
    record PlayerState(String playerId, int remainingDice, int chips) {}
}
