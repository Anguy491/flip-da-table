package com.flip.backend.game.engine;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Result of command validation. No side effects. */
public final class ValidationResult {
    private final boolean ok;
    private final List<Item> errors;

    public record Item(EngineErrorCode code, String message) {
        public Item {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }

    private ValidationResult(boolean ok, List<Item> errors) {
        this.ok = ok;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(EngineErrorCode code, String message) {
        return new ValidationResult(false, List.of(new Item(code, message)));
    }

    public static ValidationResult fail(List<Item> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("errors must not be empty");
        }
        return new ValidationResult(false, List.copyOf(errors));
    }

    public boolean isOk() { return ok; }
    public List<Item> errors() { return Collections.unmodifiableList(errors); }
}
