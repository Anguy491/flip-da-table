package com.flip.backend.lasvegas.entities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public state for one numbered casino. */
public final class LasVegasCasino {
    public record Placement(int regularDice, boolean bigDie) {
        public Placement {
            if (regularDice < 0) throw new IllegalArgumentException("regularDice must be >= 0");
        }

        public int influence() { return regularDice + (bigDie ? 2 : 0); }
        public int diceCount() { return regularDice + (bigDie ? 1 : 0); }
        public Placement add(int regular, boolean big) {
            return new Placement(regularDice + regular, bigDie || big);
        }
        public Placement add(Placement other) {
            return add(other.regularDice(), other.bigDie());
        }
    }

    private final int number;
    private LasVegasMoneyCard jackpot;
    private LasVegasMoneyCard secondPrize;
    private final Map<String, Placement> placements = new LinkedHashMap<>();

    public LasVegasCasino(int number) {
        if (number < 1 || number > 6) throw new IllegalArgumentException("casino number must be 1-6");
        this.number = number;
    }

    public int number() { return number; }
    public LasVegasMoneyCard jackpot() { return jackpot; }
    public LasVegasMoneyCard secondPrize() { return secondPrize; }
    public Map<String, Placement> placements() { return Map.copyOf(placements); }

    public void setBonuses(LasVegasMoneyCard first, LasVegasMoneyCard second) {
        if (first == null || second == null) throw new IllegalArgumentException("two bonus cards are required");
        jackpot = first.amount() >= second.amount() ? first : second;
        secondPrize = first.amount() >= second.amount() ? second : first;
    }

    public List<LasVegasMoneyCard> bonuses() {
        return jackpot == null ? List.of() : List.of(jackpot, secondPrize);
    }

    public void place(String playerId, int regularDice, boolean bigDie) {
        if (regularDice == 0 && !bigDie) throw new IllegalArgumentException("at least one die is required");
        placements.merge(playerId, new Placement(regularDice, bigDie), Placement::add);
    }

    public void clearForNextRound() {
        jackpot = null;
        secondPrize = null;
        placements.clear();
    }

    public void restore(int jackpotAmount, int secondAmount, Map<String, Placement> restoredPlacements) {
        setBonuses(new LasVegasMoneyCard(jackpotAmount), new LasVegasMoneyCard(secondAmount));
        placements.clear();
        if (restoredPlacements != null) placements.putAll(restoredPlacements);
    }
}
