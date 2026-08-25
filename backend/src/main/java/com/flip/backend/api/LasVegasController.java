package com.flip.backend.api;

import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import com.flip.backend.lasvegas.engine.view.LasVegasView.GameView;
import com.flip.backend.lasvegas.engine.view.LasVegasView.PublicEvent;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.service.game.LasVegasGameService;
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
@RequestMapping("/api/games/las-vegas")
public class LasVegasController {
    public record CommandRequest(@Min(0) long expectedVersion, @NotBlank String type, Integer face) {}
    public record CommandResponse(boolean applied, GameView view, List<PublicEvent> publicEvents) {}
    public record AssetVisibilityRequest(boolean visible) {}
    public record AssetVisibilityResponse(GameView view, PublicEvent publicEvent) {}

    private final LasVegasGameService games;
    private final GameAccessService access;

    public LasVegasController(LasVegasGameService games, GameAccessService access) {
        this.games = games;
        this.access = access;
    }

    @GetMapping("/{gameId}/view")
    public ResponseEntity<GameView> view(
            @PathVariable String gameId,
            Authentication authentication
    ) {
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
        var outcome = games.command(
                gameId,
                playerId,
                new LasVegasRuntimePhase.Command(request.expectedVersion(), request.type(), request.face())
        );
        return ResponseEntity.ok(new CommandResponse(true, outcome.view(), outcome.publicEvents()));
    }

    @PostMapping("/{gameId}/presentation/assets")
    public ResponseEntity<AssetVisibilityResponse> setAssetVisibility(
            @PathVariable String gameId,
            @RequestBody AssetVisibilityRequest request,
            Authentication authentication
    ) {
        String playerId = access.requirePlayer(authentication, gameId);
        var outcome = games.setAssetVisibility(gameId, playerId, request.visible());
        return ResponseEntity.ok(new AssetVisibilityResponse(outcome.view(), outcome.publicEvent()));
    }
}
