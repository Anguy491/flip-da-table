package com.flip.backend.api;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/sessions/{sessionId}")
public class GameController {
    private final GameService svc;
    public GameController(GameService svc) { this.svc = svc; }

    @PostMapping("/start")
    public ResponseEntity<StartGameResponse> startFirst(
            @PathVariable String sessionId,
            @Valid @RequestBody StartGameRequest req) {
        return ResponseEntity.ok(svc.startFirst(sessionId, req));
    }
}
