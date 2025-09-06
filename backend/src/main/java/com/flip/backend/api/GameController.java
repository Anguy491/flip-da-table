package com.flip.backend.api;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.service.game.GameService;
import com.flip.backend.persistence.SessionMemberRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.flip.backend.persistence.SessionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/sessions/{sessionId}")
public class GameController {
    private final SessionRepository sessions;
    private final java.util.List<GameService> services;
    private final SessionMemberRepository members;
    private final SimpMessagingTemplate messaging;
    public GameController(SessionRepository sessions, java.util.List<GameService> services, SessionMemberRepository members, SimpMessagingTemplate messaging) {
        this.sessions = sessions;
        this.services = services;
        this.members = members;
        this.messaging = messaging;
    }

    @PostMapping("/start")
    public ResponseEntity<StartGameResponse> startFirst(
            @PathVariable String sessionId,
            @Valid @RequestBody StartGameRequest req) {
        var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
        String type = session.getGameType();
        GameService svc = services.stream().filter(s -> s.supports(type)).findFirst()
            .orElseThrow(() -> new IllegalStateException("No service for game type: "+type));
        var resp = svc.startFirst(sessionId, req);
        // Broadcast start to all members with per-user myPlayerId mapping by nickname
        var mlist = members.findBySessionId(sessionId);
        for (var m : mlist) {
            String nick = m.getNickname();
            String my = resp.players().stream().filter(p -> nick.equals(p.name())).map(PlayerStartInfo::playerId).findFirst().orElse(null);
            var payload = new StartGameResponse(resp.gameId(), resp.roundIndex(), my, resp.players(), resp.view());
            messaging.convertAndSend("/topic/lobby/" + sessionId + "/" + m.getUserId(), payload);
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/start/next")
    public ResponseEntity<StartGameResponse> startNext(
        @PathVariable String sessionId,
        @Valid @RequestBody StartGameRequest req) {
        var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
        String type = session.getGameType();
        GameService svc = services.stream().filter(s -> s.supports(type)).findFirst()
            .orElseThrow(() -> new IllegalStateException("No service for game type: "+type));
        return ResponseEntity.ok(svc.startNext(sessionId, req));
    }
}
