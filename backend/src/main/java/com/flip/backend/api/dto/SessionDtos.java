package com.flip.backend.api.dto;

import jakarta.validation.constraints.*;

public class SessionDtos {
    public record CreateSessionRequest(
        @NotBlank String gameType,              // "DAVINCI" | "UNO" | "BOUNTY"
        @Min(2) @Max(12) int maxPlayers
    ) {}
    public record CreateSessionResponse(String sessionId) {}
}
