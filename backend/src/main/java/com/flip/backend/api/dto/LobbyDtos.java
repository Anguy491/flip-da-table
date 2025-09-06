package com.flip.backend.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class LobbyDtos {
    // For now we use Object for polymorphic game view payload (UnoView, DVCView, etc.)
    public record PlayerSpec(
        @NotBlank String name,
        boolean bot,
        boolean ready
    ) {}

    public record StartGameRequest(
        @Min(1) @Max(10) int rounds,
        @Size(min=1, max=10) List<PlayerSpec> players
    ) {}

    /** Player info returned when a game starts (e.g. UNO). */
    public record PlayerStartInfo(
        String playerId,
        String name,
        boolean bot,
        boolean ready
    ) {}

    /** Start game response (generic). Some fields may be null for certain game types. */
    public record StartGameResponse(
        String gameId,
        int roundIndex,
        String myPlayerId,
        List<PlayerStartInfo> players,
        Object view // UnoView or DVCView
    ) {}

    public record SessionView(
        String id, Long ownerId, String gameType, int maxPlayers,
        List<LobbyPlayer> players
    ) {}

    public record LobbyPlayer(Long userId, String nickname) {}
}
