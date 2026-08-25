package com.flip.backend.service.game;

/** Server-authoritative lobby and lifecycle constraints for a game type. */
public record GameCapabilities(
        int minPlayers,
        int maxPlayers,
        boolean botsAllowed,
        boolean seriesAllowed,
        int internalRounds
) {
    public GameCapabilities {
        if (minPlayers < 1 || maxPlayers < minPlayers || internalRounds < 1) {
            throw new IllegalArgumentException("invalid game capabilities");
        }
    }

    public static GameCapabilities forGameType(String gameType) {
        String normalized = gameType == null ? "" : gameType.trim().toUpperCase();
        return switch (normalized) {
            case "UNO" -> new GameCapabilities(2, 10, true, true, 1);
            case "DAVINCI" -> new GameCapabilities(2, 4, true, true, 1);
            case "LASVEGAS" -> new GameCapabilities(3, 10, false, false, 3);
            default -> throw new IllegalArgumentException("unsupported game type");
        };
    }
}
