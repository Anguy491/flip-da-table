package com.flip.backend.api;

import com.flip.backend.persistence.GameRepository;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.service.game.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class LatestGameController {
    public record LatestGameResponse(
            String gameId,
            String gameType,
            String state,
            int roundIndex,
            String myPlayerId,
            Object view
    ) {}

    private final GameRepository games;
    private final List<GameService> services;
    private final GameAccessService access;

    public LatestGameController(GameRepository games, List<GameService> services, GameAccessService access) {
        this.games = games;
        this.services = services;
        this.access = access;
    }

    @GetMapping("/{sessionId}/latest-game")
    public ResponseEntity<LatestGameResponse> latest(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        access.requireSessionMember(authentication, sessionId);
        var game = games.findTopBySessionIdAndStateInOrderByRoundIndexDesc(sessionId, List.of("RUNNING", "ENDED"))
                .orElseThrow(() -> new IllegalArgumentException("no game found for session"));
        String playerId = access.requirePlayer(authentication, game.getId());
        GameService service = services.stream().filter(candidate -> candidate.supports(game.getGameType()))
                .findFirst().orElseThrow(() -> new IllegalStateException("no service for game type"));
        return ResponseEntity.ok(new LatestGameResponse(
                game.getId(),
                game.getGameType(),
                game.getState(),
                game.getRoundIndex(),
                playerId,
                service.viewFor(game.getId(), playerId)
        ));
    }
}
