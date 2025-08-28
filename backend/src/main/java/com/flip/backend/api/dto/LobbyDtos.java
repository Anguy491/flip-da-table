package com.flip.backend.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import com.flip.backend.uno.engine.view.UnoView; // optional, only populated for UNO

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
        UnoView view // initial perspective view (UNO only for now)
    ) {}

    public record SessionView(
        String id, Long ownerId, String gameType, int maxPlayers
    ) {}
}
