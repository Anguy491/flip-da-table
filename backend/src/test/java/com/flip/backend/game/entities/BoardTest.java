package com.flip.backend.game.entities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TestPlayer extends Player { public TestPlayer(String id){ super(id);} }

public class BoardTest {

    private Board<TestPlayer> make4() { return new Board<>(List.of(new TestPlayer("P0"), new TestPlayer("P1"), new TestPlayer("P2"), new TestPlayer("P3"))); }

    @Test
    void rotationAndReverse() {
        Board<TestPlayer> b = make4();
        assertEquals("P0", b.currentPlayer().getId());
        b.step(1); assertEquals("P1", b.currentPlayer().getId());
        b.step(1); assertEquals("P2", b.currentPlayer().getId());
        b.step(1); assertEquals("P3", b.currentPlayer().getId());
        b.step(1); assertEquals("P0", b.currentPlayer().getId());
        b.reverse();
        b.step(1); assertEquals("P3", b.currentPlayer().getId());
        b.step(1); assertEquals("P2", b.currentPlayer().getId());
    }

    @Test
    void stepTwoSkipSemantic() {
        Board<TestPlayer> b = make4();
        b.step(2); assertEquals("P2", b.currentPlayer().getId());
        b.reverse(); // now direction -1
        b.step(3); // backwards two seats: P2 -> P1 -> P0
        assertEquals("P3", b.currentPlayer().getId());
    }

    @Test
    void twoPlayerReverseExternalEquivalence() {
        Board<TestPlayer> b = new Board<>(List.of(new TestPlayer("A"), new TestPlayer("B")));
        assertEquals("A", b.currentPlayer().getId());
        b.reverse(); // should not move pointer
        assertEquals("A", b.currentPlayer().getId());
        b.step(1); // external logic for 2-player reverse equivalence
        assertEquals("B", b.currentPlayer().getId());
    }

    @Test
    void turnCounting() {
        Board<TestPlayer> b = make4();
        assertEquals(0, b.turnCount());
        b.step(1); // still same turn
        assertEquals(0, b.turnCount());
        b.tickTurn();
        assertEquals(1, b.turnCount());
        b.step(2);
        b.tickTurn();
        assertEquals(2, b.turnCount());
    }

    @Test
    void stability() {
        Board<TestPlayer> b = make4();
        int dir = b.direction();
        b.reverse(); b.reverse();
        assertEquals(dir, b.direction());
        for (int i=0;i<4;i++) b.step(1);
        assertEquals("P0", b.currentPlayer().getId());
    }
}
