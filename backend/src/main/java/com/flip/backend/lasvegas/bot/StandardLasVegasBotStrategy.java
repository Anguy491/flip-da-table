package com.flip.backend.lasvegas.bot;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic, explainable strategy based only on the public table and the bot's current roll. */
@Component
public final class StandardLasVegasBotStrategy implements LasVegasBotStrategy {
    @Override
    public Decision choose(TurnState state) {
        if (state == null || state.currentRoll() == null || state.currentRoll().isEmpty()) {
            throw new IllegalArgumentException("a current roll is required");
        }
        var faces = state.currentRoll().stream().map(DieState::face).distinct().sorted().toList();
        var candidates = new ArrayList<Candidate>(faces.size());
        for (int face : faces) candidates.add(score(state, face));

        boolean everyPlacementIsAZeroValueElimination = candidates.stream()
                .allMatch(candidate -> candidate.prizeAmount() == 0 && !candidate.eligible());
        if (state.chips() > 0 && everyPlacementIsAZeroValueElimination) return Decision.skip();

        Candidate best = candidates.stream().max(CANDIDATE_ORDER).orElseThrow();
        return Decision.place(best.face());
    }

    private Candidate score(TurnState state, int face) {
        CasinoState casino = state.casinos().stream()
                .filter(candidate -> candidate.number() == face)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("casino is missing for face " + face));
        int regularDice = (int) state.currentRoll().stream().filter(die -> die.face() == face && !die.big()).count();
        boolean usesBigDie = state.currentRoll().stream().anyMatch(die -> die.face() == face && die.big());
        int addedInfluence = regularDice + (usesBigDie ? 2 : 0);
        int physicalDice = regularDice + (usesBigDie ? 1 : 0);

        Map<String, Integer> influences = new HashMap<>();
        for (PlacementState placement : casino.placements()) {
            influences.merge(placement.playerId(), placement.influence(), Integer::sum);
        }
        influences.merge(state.botId(), addedInfluence, Integer::sum);

        Map<Integer, Integer> counts = new HashMap<>();
        influences.values().stream().filter(value -> value > 0).forEach(value -> counts.merge(value, 1, Integer::sum));
        Set<String> eliminated = new HashSet<>();
        influences.forEach((playerId, influence) -> {
            if (influence > 0 && counts.getOrDefault(influence, 0) > 1) eliminated.add(playerId);
        });
        boolean eligible = !eliminated.contains(state.botId());
        var eligiblePlayers = influences.entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && !eliminated.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .toList();
        int prizeAmount = 0;
        if (eligible) {
            for (int index = 0; index < eligiblePlayers.size() && index < casino.bonuses().size(); index++) {
                if (eligiblePlayers.get(index).getKey().equals(state.botId())) {
                    prizeAmount = casino.bonuses().get(index);
                    break;
                }
            }
        }

        int ownInfluence = influences.getOrDefault(state.botId(), 0);
        int safetyMargin = influences.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(state.botId()) && entry.getValue() > 0)
                .mapToInt(entry -> Math.abs(ownInfluence - entry.getValue()))
                .min()
                .orElse(ownInfluence);
        int casinoTotal = casino.bonuses().stream().mapToInt(Integer::intValue).sum();
        return new Candidate(face, prizeAmount, eligible, safetyMargin, physicalDice, usesBigDie, casinoTotal);
    }

    private static final Comparator<Candidate> CANDIDATE_ORDER = Comparator
            .comparingInt(Candidate::prizeAmount)
            .thenComparing(Candidate::eligible)
            .thenComparingInt(Candidate::safetyMargin)
            .thenComparing(Comparator.comparingInt(Candidate::physicalDice).reversed())
            .thenComparing(candidate -> !candidate.usesBigDie())
            .thenComparingInt(Candidate::casinoTotal)
            .thenComparing(Comparator.comparingInt(Candidate::face).reversed());

    private record Candidate(
            int face,
            int prizeAmount,
            boolean eligible,
            int safetyMargin,
            int physicalDice,
            boolean usesBigDie,
            int casinoTotal
    ) {}
}
