package com.flip.backend.conquerwesteros.entities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public sealed interface BattleLine permits BattleLine.Military, BattleLine.Symbols {
    String id();
    String display();
    boolean matches(List<DieFace> faces);

    record Military(String id, int threshold) implements BattleLine {
        public Military {
            Objects.requireNonNull(id, "id");
            if (threshold < 1) throw new IllegalArgumentException("threshold must be positive");
        }

        @Override public String display() { return "Military ≥ " + threshold; }

        @Override
        public boolean matches(List<DieFace> faces) {
            return faces != null
                    && !faces.isEmpty()
                    && faces.stream().allMatch(DieFace::isMilitary)
                    && faces.stream().mapToInt(DieFace::militaryStrength).sum() >= threshold;
        }
    }

    record Symbols(String id, List<DieFace> required) implements BattleLine {
        public Symbols {
            Objects.requireNonNull(id, "id");
            required = List.copyOf(Objects.requireNonNull(required, "required"));
            if (required.isEmpty() || required.stream().anyMatch(DieFace::isMilitary)) {
                throw new IllegalArgumentException("symbol lines require non-military faces");
            }
        }

        @Override
        public String display() {
            return required.stream().map(DieFace::display).reduce((left, right) -> left + " + " + right).orElse("");
        }

        @Override
        public boolean matches(List<DieFace> faces) {
            if (faces == null || faces.size() != required.size()) return false;
            Comparator<DieFace> order = Comparator.comparingInt(DieFace::ordinal);
            var actual = new ArrayList<>(faces);
            var expected = new ArrayList<>(required);
            actual.sort(order);
            expected.sort(order);
            return actual.equals(expected);
        }
    }
}
