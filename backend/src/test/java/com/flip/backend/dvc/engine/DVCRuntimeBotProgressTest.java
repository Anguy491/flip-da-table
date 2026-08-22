package com.flip.backend.dvc.engine;

import com.flip.backend.dvc.engine.phase.DVCRuntimePhase;
import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import com.flip.backend.dvc.entities.DVCCard;
import com.flip.backend.dvc.entities.DVCPlayer;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DVCRuntimeBotProgressTest {

    @RepeatedTest(25)
    void botCompletesItsWholeTurnWithoutClientRequests() {
        DVCStartPhase start = readyGame(List.of("P1_HUMAN", "BOT1"));
        DVCRuntimePhase runtime = start.transit();
        runtime.enter();

        DVCPlayer human = player(start, "P1_HUMAN");
        DVCPlayer bot = player(start, "BOT1");

        for (int round = 1; round <= 3; round++) {
            assertEquals(human.getId(), runtime.board().currentPlayer().getId());
            assertEquals(DVCRuntimePhase.Awaiting.DRAW_COLOR, runtime.awaiting());

            assertTrue(runtime.provideDrawColor(human.getId(), "BLACK"));
            makeDeliberatelyWrongGuess(runtime, human, bot);
            assertEquals(DVCRuntimePhase.Awaiting.SETTLE_POSITION, runtime.awaiting());
            var humanGuess = runtime.actionLogSnapshot().stream()
                .filter(entry -> entry.type().equals("GUESS") && entry.actorId().equals(human.getId()))
                .reduce((first, second) -> second)
                .orElseThrow();
            assertFalse(humanGuess.correct());
            assertEquals(bot.getId(), humanGuess.targetPlayerId());
            assertNotNull(humanGuess.targetPosition());
            assertTrue(humanGuess.text().contains("WRONG"));

            assertTrue(runtime.provideSettlePosition(human.getId(), null));

            assertFalse(runtime.isFinished());
            assertEquals(human.getId(), runtime.board().currentPlayer().getId(),
                "the bot must return control to the human after finishing its turn");
            assertEquals(DVCRuntimePhase.Awaiting.DRAW_COLOR, runtime.awaiting());
            assertEquals(round * 2L, runtime.turnId(), "both the human and bot turns must advance");
            assertNull(runtime.board().getPending(bot.getId()), "the bot must settle its drawn card");
            assertEquals(round, runtime.actionLogSnapshot().stream()
                .filter(entry -> entry.type().equals("GUESS") && entry.actorId().equals(bot.getId()))
                .count(), "each bot turn must record one guess outcome");
            assertEquals(runtime.actionLogSnapshot(), runtime.buildView(human.getId()).actionLog());
        }
    }

    @RepeatedTest(25)
    void allBotGameRunsToCompletionWithoutAnyExternalInput() {
        DVCStartPhase start = readyGame(List.of("BOT1", "BOT2", "BOT3", "BOT4"));
        DVCRuntimePhase runtime = start.transit();

        assertDoesNotThrow(runtime::enter);

        assertTrue(runtime.isFinished());
        assertNotNull(runtime.winnerId());
        assertEquals(DVCRuntimePhase.Awaiting.NONE, runtime.awaiting());
        assertTrue(runtime.turnId() > 0);
        assertTrue(runtime.actionLogSnapshot().size() <= 50);
        assertTrue(runtime.actionLogSnapshot().stream().anyMatch(entry -> entry.type().equals("WIN")));
    }

    private static DVCStartPhase readyGame(List<String> playerIds) {
        DVCStartPhase start = new DVCStartPhase(playerIds);
        start.enter();
        for (DVCPlayer player : start.players()) start.settled(player.getId());
        assertTrue(start.allSettled());
        return start;
    }

    private static DVCPlayer player(DVCStartPhase start, String id) {
        return start.players().stream()
            .filter(candidate -> candidate.getId().equals(id))
            .findFirst()
            .orElseThrow();
    }

    private static void makeDeliberatelyWrongGuess(
        DVCRuntimePhase runtime,
        DVCPlayer actor,
        DVCPlayer target
    ) {
        List<DVCCard> cards = target.hand().snapshot();
        for (int index = 0; index < cards.size(); index++) {
            DVCCard card = cards.get(index);
            if (card.isFaceUp()) continue;
            boolean accepted = card.isJoker()
                ? runtime.provideGuess(actor.getId(), target.getId(), index, false, 0)
                : runtime.provideGuess(
                    actor.getId(),
                    target.getId(),
                    index,
                    false,
                    (card.getNumber() + 1) % 12
                );
            assertTrue(accepted);
            return;
        }
        fail("expected the bot to have an unrevealed card");
    }
}
