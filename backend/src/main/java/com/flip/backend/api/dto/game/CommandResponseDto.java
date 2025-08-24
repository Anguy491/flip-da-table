package com.flip.backend.api.dto.game;

import java.util.List;

public record CommandResponseDto(
        boolean applied,
        List<ValidationError> errors,
        List<?> events,
        Object view
) {
    public record ValidationError(String code, String message) {}
}
