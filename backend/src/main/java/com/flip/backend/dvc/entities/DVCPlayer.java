package com.flip.backend.dvc.entities;

import com.flip.backend.game.entities.Player;
import java.util.List;
import java.util.Objects;

/**
 * DVC player: holds a hand of ordered hidden/revealed cards.
 */
public class DVCPlayer extends Player {
    private final DVCHand hand = new DVCHand();

    public DVCPlayer(String id) { super(id, false); }
    public DVCPlayer(String id, boolean bot) { super(id, bot); }

    public DVCHand hand() { return hand; }

    /** Deal (ordered insertion). */
    public void giveCard(DVCCard c) { hand.addOrdered(c); }
    /** Initial deal without ordering (player will reorder manually). */
    public void dealRaw(DVCCard c) { hand.addRaw(c); }

    public int cardCount() { return hand.size(); }

    /** How many still hidden (not revealed). */
    public long hiddenCount() { return hand.snapshot().stream().filter(c -> !c.isRevealed()).count(); }

    /** Reveal a card at a given position (index validation). */
    public DVCCard revealAt(int index) {
        List<DVCCard> list = hand.snapshot();
        if (index < 0 || index >= list.size()) throw new IndexOutOfBoundsException();
        DVCCard c = list.get(index); c.reveal(); return c;
    }

    /** Attempt guess verification: returns true if index matches guess (number or JOKER). */
    public boolean verifyGuess(int index, Guess guess) {
        List<DVCCard> list = hand.snapshot();
        if (index < 0 || index >= list.size()) return false;
        DVCCard target = list.get(index);
        if (target.isRevealed()) return false; // already revealed cannot be guessed again
        if (guess.isJoker()) return target.isJoker();
        return !target.isJoker() && Objects.equals(target.getNumber(), guess.number());
    }

    /** Simple guess DTO. */
    public record Guess(Integer number, boolean isJoker) {
        public static Guess number(int n) { return new Guess(n, false); }
        public static Guess jokerGuess() { return new Guess(null, true); }
    }
}
