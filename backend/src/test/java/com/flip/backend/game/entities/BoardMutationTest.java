package com.flip.backend.game.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MutationPlayer extends Player { public MutationPlayer(String id){ super(id);} }

public class BoardMutationTest {

    private Board<MutationPlayer> base() {
        return new Board<>(List.of(new MutationPlayer("A"), new MutationPlayer("B"), new MutationPlayer("C")));
    }

    @Test
    void insertAfterAndOrder() {
        Board<MutationPlayer> b = base();
        b.insertAfter("B", new MutationPlayer("X")); // A B X C (current=A)
        b.step(1); // B
        b.step(1); // X
        assertEquals("X", b.currentPlayer().getId());
        assertEquals(4, b.size());
    }

    @Test
    void removeNonCurrentAndCurrent() {
        Board<MutationPlayer> b = base(); // current A
        assertTrue(b.remove("B")); // remove middle
        assertEquals(2, b.size());
        // Now removing any would break invariant -> expect exception when trying
        Executable ex = () -> b.remove("C");
        assertThrows(IllegalStateException.class, ex);
    }

    @Test
    void removeCurrentAdvancesPointer() {
        Board<MutationPlayer> b = base(); // A current
        assertTrue(b.remove("A")); // now current should be former B
        assertEquals("B", b.currentPlayer().getId());
        assertEquals(2, b.size());
    }

    @Test
    void insertAfterUnknownThrows() {
        Board<MutationPlayer> b = base();
        assertThrows(java.util.NoSuchElementException.class, () -> b.insertAfter("Z", new MutationPlayer("N")));
    }
}
