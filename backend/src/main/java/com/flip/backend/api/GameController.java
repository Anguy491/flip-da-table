package com.flip.backend.api;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.service.game.GameService;
import com.flip.backend.persistence.SessionMemberRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.security.GameAccessService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@RestController @RequestMapping("/api/sessions/{sessionId}")
public class GameController {
    private final java.util.List<GameService> services;
    private final SessionMemberRepository members;
    private final SimpMessagingTemplate messaging;
    private final GameAccessService access;
    public GameController(java.util.List<GameService> services, SessionMemberRepository members, SimpMessagingTemplate messaging, GameAccessService access) {
        this.services = services;
        this.members = members;
        this.messaging = messaging;
        this.access = access;
    }

    @PostMapping("/start")
    @Transactional
    public ResponseEntity<StartGameResponse> startFirst(
            @PathVariable String sessionId,
            @Valid @RequestBody StartGameRequest req,
            Authentication authentication) {
        SessionEntity session = access.requireOwner(authentication, sessionId);
        return start(session, req, authentication, false);
    }

    @PostMapping("/start/next")
    @Transactional
    public ResponseEntity<StartGameResponse> startNext(
        @PathVariable String sessionId,
        @Valid @RequestBody StartGameRequest req,
        Authentication authentication) {
        SessionEntity session = access.requireOwner(authentication, sessionId);
        return start(session, req, authentication, true);
    }

    private ResponseEntity<StartGameResponse> start(
            SessionEntity session,
            StartGameRequest requested,
            Authentication authentication,
            boolean nextRound
    ) {
        GameService service = services.stream().filter(candidate -> candidate.supports(session.getGameType())).findFirst()
                .orElseThrow(() -> new IllegalStateException("No service for game type: " + session.getGameType()));
        var sessionMembers = members.findBySessionIdOrderByJoinedAtAscIdAsc(session.getId());
        StartGameRequest authoritative = authoritativeRequest(session, sessionMembers, requested);
        StartGameResponse started = nextRound
                ? service.startNext(session.getId(), authoritative)
                : service.startFirst(session.getId(), authoritative);

        access.registerPlayers(started.gameId(), sessionMembers, started.players());
        var launchMessages = new ArrayList<LaunchMessage>();
        for (var member : sessionMembers) {
            String playerId = access.playerIdForUser(started.gameId(), member.getUserId());
            var payload = responseFor(service, started, playerId);
            launchMessages.add(new LaunchMessage(
                    "/topic/lobby/" + session.getId() + "/" + member.getUserId(),
                    payload
            ));
        }
        afterCommit(() -> launchMessages.forEach(message -> messaging.convertAndSend(message.destination(), message.payload())));
        String ownerPlayerId = access.requirePlayer(authentication, started.gameId());
        return ResponseEntity.ok(responseFor(service, started, ownerPlayerId));
    }

    private StartGameRequest authoritativeRequest(
            SessionEntity session,
            List<com.flip.backend.persistence.SessionMemberEntity> sessionMembers,
            StartGameRequest requested
    ) {
        var players = new ArrayList<PlayerSpec>();
        for (var member : sessionMembers) {
            players.add(new PlayerSpec(member.getNickname(), false, true));
        }
        long requestedBots = requested.players() == null
                ? 0
                : requested.players().stream().filter(PlayerSpec::bot).count();
        if (sessionMembers.size() + requestedBots > session.getMaxPlayers()) {
            throw new IllegalArgumentException("player count exceeds room capacity");
        }
        for (int i = 1; i <= requestedBots; i++) {
            players.add(new PlayerSpec("Bot " + i, true, true));
        }
        return new StartGameRequest(requested.rounds(), List.copyOf(players));
    }

    private StartGameResponse responseFor(GameService service, StartGameResponse started, String playerId) {
        return new StartGameResponse(
                started.gameId(),
                started.roundIndex(),
                playerId,
                started.players(),
                service.viewFor(started.gameId(), playerId)
        );
    }

    private void afterCommit(Runnable callback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            callback.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { callback.run(); }
        });
    }

    private record LaunchMessage(String destination, StartGameResponse payload) {}
}
