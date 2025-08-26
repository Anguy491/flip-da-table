package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnoBotTest {
    @Test
    void botFlagPropagation() {
        UnoBot bot = new UnoBot("BOT#1");
        assertTrue(bot.isBot());
        assertEquals("BOT#1", bot.getId());
        assertEquals(0, bot.cardCount());
    }
}
