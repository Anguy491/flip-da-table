package com.flip.backend.lasvegas.entities;

import com.flip.backend.game.entities.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LasVegasPlayer extends Player {
    public static final int REGULAR_DICE = 7;

    private final String name;
    private int chips;
    private int remainingRegularDice = REGULAR_DICE;
    private boolean bigDieRemaining = true;
    private final List<LasVegasMoneyCard> moneyCards = new ArrayList<>();

    public LasVegasPlayer(String id, String name) {
        super(id, false);
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() { return name; }
    public int chips() { return chips; }
    public int remainingRegularDice() { return remainingRegularDice; }
    public boolean bigDieRemaining() { return bigDieRemaining; }
    public int remainingDiceCount() { return remainingRegularDice + (bigDieRemaining ? 1 : 0); }
    public boolean hasDiceRemaining() { return remainingDiceCount() > 0; }
    public List<LasVegasMoneyCard> moneyCards() { return List.copyOf(moneyCards); }
    public int moneyCardCount() { return moneyCards.size(); }
    public int cashTotal() { return moneyCards.stream().mapToInt(LasVegasMoneyCard::amount).sum(); }
    public int totalAssets() { return cashTotal() + chips * 10_000; }
    public int tieBreakCount() { return moneyCardCount() + chips; }

    public void addRoundChips() { chips += 2; }

    public void spendChip() {
        if (chips < 1) throw new IllegalStateException("no chips available");
        chips--;
    }

    public void placeDice(int regularDice, boolean bigDie) {
        if (regularDice < 0 || regularDice > remainingRegularDice) {
            throw new IllegalArgumentException("invalid regular dice count");
        }
        if (bigDie && !bigDieRemaining) throw new IllegalArgumentException("big die is not available");
        remainingRegularDice -= regularDice;
        if (bigDie) bigDieRemaining = false;
    }

    public void award(LasVegasMoneyCard card) {
        moneyCards.add(Objects.requireNonNull(card, "card"));
    }

    public void resetDice() {
        remainingRegularDice = REGULAR_DICE;
        bigDieRemaining = true;
    }

    public void restore(int restoredChips, int regularDice, boolean bigRemaining, List<Integer> cardAmounts) {
        if (restoredChips < 0 || regularDice < 0 || regularDice > REGULAR_DICE) {
            throw new IllegalArgumentException("invalid persisted player state");
        }
        chips = restoredChips;
        remainingRegularDice = regularDice;
        bigDieRemaining = bigRemaining;
        moneyCards.clear();
        if (cardAmounts != null) cardAmounts.stream().map(LasVegasMoneyCard::new).forEach(moneyCards::add);
    }
}
