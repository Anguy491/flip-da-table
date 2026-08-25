package com.flip.backend.lasvegas.entities;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LasVegasMoneyDeckTest {
    @Test
    void containsTheOfficialNinetyCardComposition() {
        var deck = new LasVegasMoneyDeck(new Random(42));
        deck.initialize();

        assertEquals(90, deck.remaining());
        Map<Integer, Long> counts = deck.snapshotAmounts().stream()
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        assertEquals(Map.of(
                30_000, 11L,
                40_000, 11L,
                50_000, 13L,
                60_000, 15L,
                70_000, 13L,
                80_000, 11L,
                90_000, 9L,
                100_000, 7L
        ), counts);
    }
}
