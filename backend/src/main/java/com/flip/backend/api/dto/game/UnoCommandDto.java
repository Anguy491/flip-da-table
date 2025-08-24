package com.flip.backend.api.dto.game;

import com.flip.backend.game.uno.UnoColor;
import com.flip.backend.game.uno.UnoValue;
import jakarta.validation.constraints.NotBlank;

public record UnoCommandDto(
        @NotBlank String type,
        @NotBlank String playerId,
        UnoColor color,
        UnoValue value
) {}
