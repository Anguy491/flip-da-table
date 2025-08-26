package com.flip.backend.api;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.service.game.GameService;
import com.flip.backend.persistence.SessionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/sessions/{sessionId}")
public class GameController {
    private final SessionRepository sessions;
    private final java.util.List<GameService> services;
    public GameController(SessionRepository sessions, java.util.List<GameService> services) {
        this.sessions = sessions;
        this.services = services;
    }

    @PostMapping("/start")
    public ResponseEntity<StartGameResponse> startFirst(
            @PathVariable String sessionId,
            @Valid @RequestBody StartGameRequest req) {
        var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
        String type = session.getGameType();
        GameService svc = services.stream().filter(s -> s.supports(type)).findFirst()
            .orElseThrow(() -> new IllegalStateException("No service for game type: "+type));
        return ResponseEntity.ok(svc.startFirst(sessionId, req));
    }
}
