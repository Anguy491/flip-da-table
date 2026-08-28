package com.flip.backend.conquerwesteros.bot;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic standard strategy. It searches only the remainder of the current siege,
 * using exact dice-outcome probabilities and public scoring information.
 */
@Component
public final class StandardConquerWesterosBotStrategy implements ConquerWesterosBotStrategy {
    private static final double EPSILON = 1.0e-12;
    private static final String[] FACES = {
            "MILITARY_1", "MILITARY_2", "MILITARY_3", "RAVEN", "KNIGHT", "CROWN"
    };
    private static final Map<String, Integer> FACE_INDEX = faceIndexes();
    private static final Map<Integer, List<RollOutcome>> OUTCOMES = new HashMap<>();

    @Override
    public Decision choose(TurnState state) {
        validate(state);
        Evaluator evaluator = new Evaluator(state);
        Candidate best = evaluator.bestForActualRoll();
        if (best == null) throw new IllegalStateException("Bot has no legal decision");
        return best.decision();
    }

    /** Deterministic legality-first fallback used if the probability evaluator fails. */
    public static Decision fallback(TurnState state) {
        validate(state);
        var candidates = new ArrayList<Decision>();
        for (TargetState target : state.targets().stream().sorted(Comparator.comparing(TargetState::id)).toList()) {
            for (LineState line : target.remainingLines().stream().sorted(Comparator.comparing(LineState::id)).toList()) {
                List<Integer> dice = matchingDieIds(line, state.currentRoll());
                if (!dice.isEmpty()) candidates.add(Decision.completeLine(target.id(), line.id(), dice));
            }
        }
        return candidates.stream()
                .min(Comparator.comparingInt((Decision decision) -> decision.dieIds().size())
                        .thenComparing(Decision::targetId)
                        .thenComparing(Decision::lineId)
                        .thenComparing(decision -> decision.dieIds().toString()))
                .orElseGet(() -> Decision.loseDie(state.currentRoll().stream()
                        .mapToInt(DieState::dieId).min().orElseThrow()));
    }

    private static void validate(TurnState state) {
        if (state == null || state.currentRoll() == null || state.currentRoll().isEmpty()) {
            throw new IllegalArgumentException("a current roll is required");
        }
        if (state.targets() == null || state.targets().isEmpty()) {
            throw new IllegalArgumentException("at least one legal target is required");
        }
    }

    private static final class Evaluator {
        private final TurnState state;
        private final Map<ProbabilityKey, Double> probabilityMemo = new HashMap<>();
        private final Map<Integer, Double> unboundUtilityMemo = new HashMap<>();

        private Evaluator(TurnState state) {
            this.state = state;
        }

        Candidate bestForActualRoll() {
            int diceCount = state.currentRoll().size();
            int[] counts = counts(state.currentRoll());
            Candidate best = loseCandidate(diceCount);
            for (TargetState target : state.targets()) {
                for (int lineIndex = 0; lineIndex < target.remainingLines().size(); lineIndex++) {
                    LineState line = target.remainingLines().get(lineIndex);
                    List<Integer> dieIds = matchingDieIds(line, state.currentRoll());
                    if (dieIds.isEmpty()) continue;
                    List<LineState> remaining = without(target.remainingLines(), lineIndex);
                    double probability = remaining.isEmpty()
                            ? 1.0
                            : captureProbability(diceCount - dieIds.size(), remaining);
                    Candidate candidate = new Candidate(
                            Decision.completeLine(target.id(), line.id(), dieIds),
                            targetUtility(target) * probability,
                            probability,
                            diceCount - dieIds.size(),
                            target.central(),
                            target.id(),
                            line.id()
                    );
                    best = better(best, candidate);
                }
            }
            return best;
        }

        private Candidate loseCandidate(int diceCount) {
            int dieId = state.currentRoll().stream().mapToInt(DieState::dieId).min().orElseThrow();
            if (diceCount <= 1) {
                return new Candidate(Decision.loseDie(dieId), 0, 0, 0, false, "~", "~");
            }
            if (state.targetLocked()) {
                TargetState target = state.targets().get(0);
                double probability = captureProbability(diceCount - 1, target.remainingLines());
                return new Candidate(Decision.loseDie(dieId), targetUtility(target) * probability,
                        probability, diceCount - 1, target.central(), target.id(), "~");
            }
            return new Candidate(Decision.loseDie(dieId), unboundExpectedUtility(diceCount - 1),
                    0, diceCount - 1, false, "~", "~");
        }

        private double unboundExpectedUtility(int diceCount) {
            if (diceCount <= 0) return 0;
            Double cached = unboundUtilityMemo.get(diceCount);
            if (cached != null) return cached;
            double expected = 0;
            for (RollOutcome roll : outcomes(diceCount)) {
                double best = diceCount > 1 ? unboundExpectedUtility(diceCount - 1) : 0;
                for (TargetState target : state.targets()) {
                    for (int lineIndex = 0; lineIndex < target.remainingLines().size(); lineIndex++) {
                        LineState line = target.remainingLines().get(lineIndex);
                        int used = matchingDieCount(line, roll.counts());
                        if (used == 0) continue;
                        List<LineState> remaining = without(target.remainingLines(), lineIndex);
                        double probability = remaining.isEmpty()
                                ? 1.0
                                : captureProbability(diceCount - used, remaining);
                        best = Math.max(best, targetUtility(target) * probability);
                    }
                }
                expected += roll.probability() * best;
            }
            unboundUtilityMemo.put(diceCount, expected);
            return expected;
        }

        private double captureProbability(int diceCount, List<LineState> remaining) {
            if (remaining.isEmpty()) return 1;
            if (diceCount <= 0) return 0;
            ProbabilityKey key = new ProbabilityKey(diceCount, lineKey(remaining));
            Double cached = probabilityMemo.get(key);
            if (cached != null) return cached;
            double expected = 0;
            for (RollOutcome roll : outcomes(diceCount)) {
                double best = diceCount > 1 ? captureProbability(diceCount - 1, remaining) : 0;
                for (int lineIndex = 0; lineIndex < remaining.size(); lineIndex++) {
                    int used = matchingDieCount(remaining.get(lineIndex), roll.counts());
                    if (used == 0) continue;
                    List<LineState> after = without(remaining, lineIndex);
                    double probability = after.isEmpty() ? 1 : captureProbability(diceCount - used, after);
                    best = Math.max(best, probability);
                }
                expected += roll.probability() * best;
            }
            probabilityMemo.put(key, expected);
            return expected;
        }

        private double targetUtility(TargetState target) {
            int missingBefore = Math.max(1, target.clanSize() - target.botOwnedClanCount());
            boolean completesClan = target.botOwnedClanCount() + 1 == target.clanSize();
            double ownVpDelta = completesClan
                    ? target.clanScore() - target.botOwnedClanFaceUpPoints()
                    : target.points();
            boolean throneTransfers = !state.botId().equals(state.ironThroneHolderId())
                    && (target.kingsLanding()
                    || (target.ownerId() != null && target.ownerId().equals(state.ironThroneHolderId())));
            double throneGain = throneTransfers ? 1 : 0;
            double denial = target.ownerId() == null ? 0 : target.points() * 0.5;
            double clanProgress = completesClan ? 0 : 0.25 * target.clanScore() / missingBefore;
            return Math.max(EPSILON, ownVpDelta + throneGain + denial + clanProgress);
        }
    }

    private static Candidate better(Candidate left, Candidate right) {
        if (left == null) return right;
        int comparison = compareDouble(right.expectedUtility(), left.expectedUtility());
        if (comparison != 0) return comparison > 0 ? right : left;
        comparison = compareDouble(right.captureProbability(), left.captureProbability());
        if (comparison != 0) return comparison > 0 ? right : left;
        if (right.remainingDice() != left.remainingDice()) return right.remainingDice() > left.remainingDice() ? right : left;
        if (right.central() != left.central()) return right.central() ? right : left;
        comparison = right.targetId().compareTo(left.targetId());
        if (comparison != 0) return comparison < 0 ? right : left;
        comparison = right.lineId().compareTo(left.lineId());
        if (comparison != 0) return comparison < 0 ? right : left;
        return right.decision().dieIds().toString().compareTo(left.decision().dieIds().toString()) < 0 ? right : left;
    }

    private static int compareDouble(double left, double right) {
        if (Math.abs(left - right) <= EPSILON) return 0;
        return Double.compare(left, right);
    }

    private static List<Integer> matchingDieIds(LineState line, List<DieState> dice) {
        if ("MILITARY".equals(line.type())) {
            int threshold = line.threshold() == null ? Integer.MAX_VALUE : line.threshold();
            var military = dice.stream().filter(die -> die.militaryStrength() > 0)
                    .sorted(Comparator.comparingInt(DieState::militaryStrength).reversed()
                            .thenComparingInt(DieState::dieId))
                    .toList();
            int total = 0;
            var result = new ArrayList<Integer>();
            for (DieState die : military) {
                result.add(die.dieId());
                total += die.militaryStrength();
                if (total >= threshold) return result.stream().sorted().toList();
            }
            return List.of();
        }
        var byFace = new LinkedHashMap<String, List<Integer>>();
        dice.stream().sorted(Comparator.comparingInt(DieState::dieId)).forEach(die ->
                byFace.computeIfAbsent(die.face(), ignored -> new ArrayList<>()).add(die.dieId()));
        var usedByFace = new HashMap<String, Integer>();
        var result = new ArrayList<Integer>();
        for (String face : line.symbols()) {
            int offset = usedByFace.getOrDefault(face, 0);
            List<Integer> available = byFace.getOrDefault(face, List.of());
            if (offset >= available.size()) return List.of();
            result.add(available.get(offset));
            usedByFace.put(face, offset + 1);
        }
        return result.stream().sorted().toList();
    }

    private static int matchingDieCount(LineState line, int[] counts) {
        if ("MILITARY".equals(line.type())) {
            int threshold = line.threshold() == null ? Integer.MAX_VALUE : line.threshold();
            int total = 0;
            int used = 0;
            for (int face = 2; face >= 0; face--) {
                for (int count = 0; count < counts[face]; count++) {
                    total += face + 1;
                    used++;
                    if (total >= threshold) return used;
                }
            }
            return 0;
        }
        int[] required = new int[FACES.length];
        for (String symbol : line.symbols()) {
            Integer index = FACE_INDEX.get(symbol);
            if (index == null) return 0;
            required[index]++;
        }
        for (int index = 0; index < required.length; index++) {
            if (required[index] > counts[index]) return 0;
        }
        return line.symbols().size();
    }

    private static int[] counts(List<DieState> dice) {
        int[] result = new int[FACES.length];
        for (DieState die : dice) {
            Integer index = FACE_INDEX.get(die.face());
            if (index == null) throw new IllegalArgumentException("unknown die face: " + die.face());
            result[index]++;
        }
        return result;
    }

    private static List<LineState> without(List<LineState> lines, int removedIndex) {
        var result = new ArrayList<LineState>(lines.size() - 1);
        for (int index = 0; index < lines.size(); index++) if (index != removedIndex) result.add(lines.get(index));
        return List.copyOf(result);
    }

    private static String lineKey(List<LineState> lines) {
        return lines.stream().map(line -> line.type() + ':' + line.threshold() + ':'
                        + line.symbols().stream().sorted().toList())
                .sorted().reduce((left, right) -> left + '|' + right).orElse("");
    }

    private static synchronized List<RollOutcome> outcomes(int diceCount) {
        return OUTCOMES.computeIfAbsent(diceCount, StandardConquerWesterosBotStrategy::buildOutcomes);
    }

    private static List<RollOutcome> buildOutcomes(int diceCount) {
        var result = new ArrayList<RollOutcome>();
        enumerateOutcomes(result, new int[FACES.length], 0, diceCount, diceCount);
        return List.copyOf(result);
    }

    private static void enumerateOutcomes(
            List<RollOutcome> result,
            int[] counts,
            int faceIndex,
            int remaining,
            int totalDice
    ) {
        if (faceIndex == FACES.length - 1) {
            counts[faceIndex] = remaining;
            double combinations = factorial(totalDice);
            for (int count : counts) combinations /= factorial(count);
            result.add(new RollOutcome(Arrays.copyOf(counts, counts.length),
                    combinations / Math.pow(FACES.length, totalDice)));
            return;
        }
        for (int count = 0; count <= remaining; count++) {
            counts[faceIndex] = count;
            enumerateOutcomes(result, counts, faceIndex + 1, remaining - count, totalDice);
        }
    }

    private static long factorial(int value) {
        long result = 1;
        for (int factor = 2; factor <= value; factor++) result *= factor;
        return result;
    }

    private static Map<String, Integer> faceIndexes() {
        var result = new HashMap<String, Integer>();
        for (int index = 0; index < FACES.length; index++) result.put(FACES[index], index);
        return Map.copyOf(result);
    }

    private record Candidate(
            Decision decision,
            double expectedUtility,
            double captureProbability,
            int remainingDice,
            boolean central,
            String targetId,
            String lineId
    ) {}

    private record ProbabilityKey(int diceCount, String remainingLines) {}
    private record RollOutcome(int[] counts, double probability) {}
}
