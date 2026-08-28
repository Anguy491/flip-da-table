package com.flip.backend.conquerwesteros.bot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConquerWesterosBotStrategyTest {
    private final StandardConquerWesterosBotStrategy strategy = new StandardConquerWesterosBotStrategy();

    @Test
    void commitsTheFewestStrongestMilitaryDiceWithStableIds() {
        var state = state(false,
                List.of(die(0, "MILITARY_2", 2), die(1, "MILITARY_1", 1), die(2, "MILITARY_3", 3),
                        die(3, "RAVEN", 0), die(4, "KNIGHT", 0), die(5, "CROWN", 0), die(6, "RAVEN", 0)),
                target("T01", 1, true, null, 3, 1, 0, 0,
                        military("L1", 5)));

        assertEquals(ConquerWesterosBotStrategy.Decision.completeLine("T01", "L1", List.of(0, 2)),
                strategy.choose(state));
    }

    @Test
    void commitsExactSymbolDiceAndUsesTheLowestMatchingIds() {
        var state = state(false,
                List.of(die(0, "RAVEN", 0), die(1, "RAVEN", 0), die(2, "KNIGHT", 0),
                        die(3, "KNIGHT", 0), die(4, "CROWN", 0), die(5, "MILITARY_1", 1), die(6, "MILITARY_2", 2)),
                target("T03", 1, true, null, 3, 1, 0, 0,
                        symbols("L1", "RAVEN", "KNIGHT")));

        assertEquals(ConquerWesterosBotStrategy.Decision.completeLine("T03", "L1", List.of(0, 2)),
                strategy.choose(state));
    }

    @Test
    void valuesImmediateClanCompletionAboveAHigherPrintedCard() {
        var roll = sevenCrowns();
        var clanCompletion = target("T01", 1, true, null, 10, 2, 1, 1, symbols("L1", "CROWN"));
        var fourPointCard = target("T14", 4, true, null, 7, 2, 0, 0, symbols("L1", "CROWN"));

        assertEquals("T01", strategy.choose(state(false, roll, clanCompletion, fourPointCard)).targetId());
    }

    @Test
    void valuesAnIronThroneTransferWhenOtherUtilityIsEqual() {
        var ordinary = target("T01", 1, true, null, 4, 2, 0, 0, symbols("L1", "CROWN"));
        var kingsLanding = new ConquerWesterosBotStrategy.TargetState(
                "T10", 1, 4, 2, 0, 0, true, null, true, List.of(symbols("L1", "CROWN")));

        assertEquals("T10", strategy.choose(state(false, sevenCrowns(), ordinary, kingsLanding)).targetId());
    }

    @Test
    void appliesBalancedStealDenialButPrefersCentralOnAnExactTie() {
        var centralThree = target("T11", 3, true, null, 4, 2, 0, 0, symbols("L1", "CROWN"));
        var stealTwo = target("T08", 2, false, "P2", 4, 2, 0, 0, symbols("L1", "CROWN"));
        assertEquals("T11", strategy.choose(state(false, sevenCrowns(), stealTwo, centralThree)).targetId());

        var stealFour = target("T14", 4, false, "P2", 4, 2, 0, 0, symbols("L1", "CROWN"));
        assertEquals("T14", strategy.choose(state(false, sevenCrowns(), stealFour, centralThree)).targetId());
    }

    @Test
    void fallbackUsesTheCheapestLegalLineThenTheLowestDieId() {
        var completable = state(false, sevenCrowns(),
                target("T02", 1, true, null, 3, 1, 0, 0, symbols("L2", "CROWN", "CROWN")),
                target("T01", 1, true, null, 3, 1, 0, 0, symbols("L1", "CROWN")));
        assertEquals(ConquerWesterosBotStrategy.Decision.completeLine("T01", "L1", List.of(0)),
                StandardConquerWesterosBotStrategy.fallback(completable));

        var noMatch = state(false,
                List.of(die(3, "MILITARY_1", 1), die(5, "MILITARY_2", 2)),
                target("T01", 1, true, null, 3, 1, 0, 0, symbols("L1", "CROWN")));
        assertEquals(ConquerWesterosBotStrategy.Decision.loseDie(3),
                StandardConquerWesterosBotStrategy.fallback(noMatch));
        assertEquals(ConquerWesterosBotStrategy.Decision.loseDie(3), strategy.choose(noMatch));
    }

    @Test
    void voluntarilyLosesADieWhenAnInefficientMatchHasLowerSiegeValue() {
        var roll = List.of(
                die(0, "MILITARY_1", 1), die(1, "MILITARY_1", 1), die(2, "MILITARY_1", 1),
                die(3, "MILITARY_1", 1), die(4, "MILITARY_1", 1), die(5, "RAVEN", 0), die(6, "KNIGHT", 0));
        var target = target("T05", 2, true, null, 5, 2, 0, 0,
                military("L1", 5), symbols("L2", "CROWN"));

        assertEquals(ConquerWesterosBotStrategy.Decision.loseDie(0), strategy.choose(state(true, roll, target)));
    }

    private static ConquerWesterosBotStrategy.TurnState state(
            boolean locked,
            List<ConquerWesterosBotStrategy.DieState> roll,
            ConquerWesterosBotStrategy.TargetState... targets
    ) {
        return new ConquerWesterosBotStrategy.TurnState("BOT1", "P1", locked, roll, List.of(targets));
    }

    private static ConquerWesterosBotStrategy.TargetState target(
            String id,
            int points,
            boolean central,
            String owner,
            int clanScore,
            int clanSize,
            int ownedCount,
            int ownedPoints,
            ConquerWesterosBotStrategy.LineState... lines
    ) {
        return new ConquerWesterosBotStrategy.TargetState(id, points, clanScore, clanSize, ownedCount, ownedPoints,
                false, owner, central, List.of(lines));
    }

    private static ConquerWesterosBotStrategy.LineState military(String id, int threshold) {
        return new ConquerWesterosBotStrategy.LineState(id, "MILITARY", threshold, List.of());
    }

    private static ConquerWesterosBotStrategy.LineState symbols(String id, String... symbols) {
        return new ConquerWesterosBotStrategy.LineState(id, "SYMBOLS", null, List.of(symbols));
    }

    private static ConquerWesterosBotStrategy.DieState die(int id, String face, int strength) {
        return new ConquerWesterosBotStrategy.DieState(id, face, strength);
    }

    private static List<ConquerWesterosBotStrategy.DieState> sevenCrowns() {
        return java.util.stream.IntStream.range(0, 7).mapToObj(id -> die(id, "CROWN", 0)).toList();
    }
}
