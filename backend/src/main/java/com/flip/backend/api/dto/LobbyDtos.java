package com.flip.backend.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class LobbyDtos {
    public record PlayerSpec(
        @NotBlank String name,
        boolean bot,
        boolean ready
    ) {}

    public record StartGameRequest(
        @Min(1) @Max(10) int rounds,
        @Size(min=1, max=10) List<PlayerSpec> players
    ) {}

    public record StartGameResponse(
        String gameId,
        int roundIndex,
        java.util.List<PlayerInfo> players,
        String gameType,
        String myPlayerId
    ) {}

    public record PlayerInfo(
        String playerId,
        String name,
        boolean bot
    ) {}

    public record SessionView(
        String id, Long ownerId, String gameType, int maxPlayers
    ) {}
}
