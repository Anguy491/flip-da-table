package com.flip.backend.dvc.engine;

import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import com.flip.backend.dvc.entities.DVCCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DVCStartPhaseBotTest {
    @Test
    void botArrangesAndConfirmsItsRackWithoutAClientRequest() {
        DVCStartPhase phase = new DVCStartPhase(List.of("P1_HUMAN", "BOT1"));

        phase.enter();

        assertFalse(phase.allSettled(), "the human still needs to confirm their rack");
        var bot = phase.players().stream().filter(player -> player.isBot()).findFirst().orElseThrow();
        assertTrue(isOrdered(bot.hand().snapshot()), "the bot rack should follow the initial ordering rule");

        phase.settled("P1_HUMAN");

        assertTrue(phase.allSettled(), "the bot must not leave the human waiting forever");
        assertDoesNotThrow(phase::transit);
    }

    private boolean isOrdered(List<DVCCard> cards) {
        DVCCard previous = null;
        for (DVCCard card : cards) {
            if (card.isJoker()) continue;
            if (previous != null && DVCCard.compareForOrder(previous, card) > 0) return false;
            previous = card;
        }
        return true;
    }
}
