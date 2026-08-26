package com.flip.backend.api;

import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.PublicEvent;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.service.game.ConquerWesterosGameService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games/conquer-westeros")
public class ConquerWesterosController {
    public record CommandRequest(
            @Min(0) long expectedVersion,
            @NotBlank String type,
            String targetId,
            String lineId,
            List<Integer> dieIds,
            Integer dieId
    ) {}
    public record CommandResponse(boolean applied, GameView view, List<PublicEvent> publicEvents) {}

    private final ConquerWesterosGameService games;
    private final GameAccessService access;

    public ConquerWesterosController(ConquerWesterosGameService games, GameAccessService access) {
        this.games = games;
        this.access = access;
    }

    @GetMapping("/{gameId}/view")
    public ResponseEntity<GameView> view(@PathVariable String gameId, Authentication authentication) {
        String playerId = access.requirePlayer(authentication, gameId);
        return ResponseEntity.ok((GameView) games.viewFor(gameId, playerId));
    }

    @PostMapping("/{gameId}/commands")
    public ResponseEntity<CommandResponse> command(
            @PathVariable String gameId,
            @Valid @RequestBody CommandRequest request,
            Authentication authentication
    ) {
        String playerId = access.requirePlayer(authentication, gameId);
        var outcome = games.command(gameId, playerId, new ConquerWesterosRuntimePhase.Command(
                request.expectedVersion(), request.type(), request.targetId(), request.lineId(), request.dieIds(), request.dieId()));
        return ResponseEntity.ok(new CommandResponse(true, outcome.view(), outcome.publicEvents()));
    }
}
