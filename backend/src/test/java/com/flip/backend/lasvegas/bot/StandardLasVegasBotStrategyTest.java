package com.flip.backend.lasvegas.bot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardLasVegasBotStrategyTest {
    private final StandardLasVegasBotStrategy strategy = new StandardLasVegasBotStrategy();

    @Test
    void choosesTheHigherProvisionalPrize() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000)),
                casino(2, List.of(80_000, 70_000))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(1), decision);
    }

    @Test
    void comparesTheCurrentSecondPrizesWhenBothChoicesAreRunnerUp() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 70_000), placement("P1", 2)),
                casino(2, List.of(100_000, 60_000), placement("P2", 2))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(1), decision);
    }

    @Test
    void avoidsATieEliminationWhenAnotherFacePays() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("P1", 2)),
                casino(2, List.of(80_000, 40_000))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(2), decision);
    }

    @Test
    void prefersEligibilityWhenNeitherChoiceCurrentlyWinsMoney() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("P1", 3), placement("P2", 2)),
                casino(2, List.of(100_000, 30_000), placement("P3", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(1), decision);
    }

    @Test
    void prefersTheLargerInfluenceSafetyMarginAfterPrizeAndEligibilityTie() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(1, false), die(2, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("BOT1", 4), placement("P1", 5)),
                casino(2, List.of(100_000, 30_000), placement("BOT1", 4), placement("P2", 3))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(2), decision);
    }

    @Test
    void usesFewerPhysicalDiceAfterEarlierScoresTie() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(2, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("BOT1", 2), placement("P1", 1)),
                casino(2, List.of(100_000, 30_000), placement("BOT1", 1), placement("P2", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(1), decision);
    }

    @Test
    void spendsAChipOnlyWhenEveryPlacementIsAZeroValueElimination() {
        var decision = strategy.choose(state(1,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("P1", 1)),
                casino(2, List.of(80_000, 40_000), placement("P2", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.skip(), decision);
    }

    @Test
    void placesInsteadOfSkippingWithoutAChip() {
        var decision = strategy.choose(state(0,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("P1", 1)),
                casino(2, List.of(80_000, 40_000), placement("P2", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(1), decision);
    }

    @Test
    void preservesTheBigDieAfterEarlierScoresTie() {
        var decision = strategy.choose(state(2,
                List.of(die(1, true), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("BOT1", 1), placement("P1", 2)),
                casino(2, List.of(100_000, 30_000), placement("BOT1", 1), placement("P1", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(2), decision);
    }

    @Test
    void prefersTheHigherCasinoTotalAfterAllEarlierScoresTie() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("BOT1", 2), placement("P1", 1)),
                casino(2, List.of(100_000, 50_000), placement("BOT1", 2), placement("P2", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(2), decision);
    }

    @Test
    void usesTheSmallerFaceAsTheDeterministicFinalTieBreak() {
        var decision = strategy.choose(state(2,
                List.of(die(1, false), die(2, false)),
                casino(1, List.of(100_000, 30_000), placement("BOT1", 2), placement("P1", 1)),
                casino(2, List.of(100_000, 30_000), placement("BOT1", 2), placement("P2", 1))
        ));

        assertEquals(LasVegasBotStrategy.Decision.place(1), decision);
    }

    private static LasVegasBotStrategy.TurnState state(
            int chips,
            List<LasVegasBotStrategy.DieState> roll,
            LasVegasBotStrategy.CasinoState... casinos
    ) {
        return new LasVegasBotStrategy.TurnState(
                "BOT1", chips, roll, List.of(casinos),
                List.of(new LasVegasBotStrategy.PlayerState("BOT1", roll.size(), chips))
        );
    }

    private static LasVegasBotStrategy.DieState die(int face, boolean big) {
        return new LasVegasBotStrategy.DieState(face, big);
    }

    private static LasVegasBotStrategy.PlacementState placement(String playerId, int influence) {
        return new LasVegasBotStrategy.PlacementState(playerId, influence);
    }

    private static LasVegasBotStrategy.CasinoState casino(
            int number,
            List<Integer> bonuses,
            LasVegasBotStrategy.PlacementState... placements
    ) {
        return new LasVegasBotStrategy.CasinoState(number, bonuses, List.of(placements));
    }
}
