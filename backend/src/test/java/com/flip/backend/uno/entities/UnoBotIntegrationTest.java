package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UnoBotIntegrationTest {
    @Test
    void botAppearsInBoardAndFlagTrue() {
        UnoBot bot = new UnoBot("BOT");
        UnoPlayer p1 = new UnoPlayer("H1");
        UnoPlayer p2 = new UnoPlayer("H2");
        UnoBoard board = new UnoBoard(List.of(p1, bot, p2));
        assertEquals(3, board.size());
        // snapshot order starting at current (p1)
        var order = board.snapshotOrder();
        assertEquals(List.of(p1, bot, p2), order);
        assertTrue(order.get(1).isBot());
    }

    @Test
    void multipleBotsOrderingAndFlags() {
        UnoBot b1 = new UnoBot("B1");
        UnoBot b2 = new UnoBot("B2");
        UnoPlayer h1 = new UnoPlayer("H1");
        UnoPlayer h2 = new UnoPlayer("H2");
        UnoBoard board = new UnoBoard(List.of(h1, b1, h2, b2));
        var order = board.snapshotOrder();
        assertEquals(List.of(h1, b1, h2, b2), order);
        assertTrue(order.get(1).isBot());
        assertTrue(order.get(3).isBot());
        assertFalse(order.get(0).isBot());
    }

    @Test
    void botHandGiveAndPlayCard() {
        UnoBot bot = new UnoBot("BOT");
    new UnoBoard(List.of(new UnoPlayer("H1"), bot)); // board context not directly used here
        UnoCard c1 = UnoCard.number(UnoCard.Color.RED, 3);
        UnoCard c2 = UnoCard.skip(UnoCard.Color.GREEN);
        bot.giveCard(c1);
        bot.giveCard(c2);
        assertEquals(2, bot.cardCount());
        assertTrue(bot.playCard(c1));
        assertEquals(1, bot.cardCount());
        assertFalse(bot.playCard(c1)); // already played
        assertEquals(1, bot.cardCount());
    }

    @Test
    void rotationIncludesBots() {
        UnoBot bot = new UnoBot("BOT");
        UnoPlayer h1 = new UnoPlayer("H1");
        UnoPlayer h2 = new UnoPlayer("H2");
        UnoBoard board = new UnoBoard(List.of(h1, bot, h2));
        assertEquals("H1", board.currentPlayer().getId());
        board.step(1); // BOT
        assertEquals("BOT", board.currentPlayer().getId());
        board.step(1); // H2
        assertEquals("H2", board.currentPlayer().getId());
        board.reverse();
        board.step(1); // back to BOT (reverse direction)
        assertEquals("BOT", board.currentPlayer().getId());
    }

    @Test
    void removeBotFromBoard() {
        UnoBot bot = new UnoBot("BOT");
        UnoPlayer h1 = new UnoPlayer("H1");
        UnoPlayer h2 = new UnoPlayer("H2");
        UnoPlayer h3 = new UnoPlayer("H3");
        UnoBoard board = new UnoBoard(List.of(h1, bot, h2, h3));
        assertEquals(4, board.size());
        assertTrue(board.remove("BOT"));
        assertEquals(3, board.size());
        // Ensure remaining order reachable via steps (no dangling links)
        for (int i = 0; i < 6; i++) { board.step(1); }
        // Not a strict order assertion (shuffle by steps) but ensure no bot id present
        var ids = board.snapshotOrder().stream().map(UnoPlayer::getId).toList();
        assertFalse(ids.contains("BOT"));
    }
}
