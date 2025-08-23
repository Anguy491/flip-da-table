package com.flip.backend.api;

import com.flip.backend.api.dto.LobbyDtos.SessionView;
import com.flip.backend.service.SessionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/sessions")
public class SessionQueryController {
    private final SessionQueryService svc;
    public SessionQueryController(SessionQueryService svc) { this.svc = svc; }

    @GetMapping("/{id}")
    public ResponseEntity<SessionView> get(@PathVariable String id) {
        return ResponseEntity.ok(svc.get(id));
    }
}
