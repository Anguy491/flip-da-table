package com.flip.backend.api;

import com.flip.backend.api.dto.SessionDtos.*;
import com.flip.backend.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService svc;
    public SessionController(SessionService svc) { this.svc = svc; }

    @PostMapping
    public ResponseEntity<CreateSessionResponse> create(@Valid @RequestBody CreateSessionRequest req) {
        var resp = svc.create(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{sessionId}/join")
    public ResponseEntity<com.flip.backend.api.dto.LobbyDtos.SessionView> join(@PathVariable String sessionId) {
        var view = svc.join(sessionId);
        return ResponseEntity.ok(view);
    }
}
