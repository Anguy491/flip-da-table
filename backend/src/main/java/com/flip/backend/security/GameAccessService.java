package com.flip.backend.security;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionMemberEntity;
import com.flip.backend.persistence.SessionMemberRepository;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
public class GameAccessService {
    private final UserRepository users;
    private final SessionRepository sessions;
    private final SessionMemberRepository members;
    private final GameRepository games;
    private final GamePlayerRegistry players;

    public GameAccessService(
            UserRepository users,
            SessionRepository sessions,
            SessionMemberRepository members,
            GameRepository games,
            GamePlayerRegistry players
    ) {
        this.users = users;
        this.sessions = sessions;
        this.members = members;
        this.games = games;
        this.players = players;
    }

    public UserEntity requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw denied();
        }
        return users.findByEmailIgnoreCase(EmailNormalizer.normalize(authentication.getName()))
                .orElseThrow(this::denied);
    }

    public SessionEntity requireOwner(Authentication authentication, String sessionId) {
        UserEntity user = requireUser(authentication);
        SessionEntity session = sessions.findById(sessionId).orElseThrow(this::denied);
        if (!user.getId().equals(session.getOwnerId())) throw denied();
        return session;
    }

    public SessionMemberEntity requireSessionMember(Authentication authentication, String sessionId) {
        UserEntity user = requireUser(authentication);
        return members.findBySessionIdAndUserId(sessionId, user.getId()).orElseThrow(this::denied);
    }

    public String requirePlayer(Authentication authentication, String gameId) {
        UserEntity user = requireUser(authentication);
        var game = games.findById(gameId).orElseThrow(this::denied);
        members.findBySessionIdAndUserId(game.getSessionId(), user.getId()).orElseThrow(this::denied);
        String playerId = players.playerId(gameId, user.getId());
        if (playerId == null) throw denied();
        return playerId;
    }

    public String requireClaimedPlayer(Authentication authentication, String gameId, String claimedPlayerId) {
        String playerId = requirePlayer(authentication, gameId);
        if (claimedPlayerId == null || !playerId.equals(claimedPlayerId)) throw denied();
        return playerId;
    }

    public Long requireUserId(Authentication authentication) {
        return requireUser(authentication).getId();
    }

    public String playerIdForUser(String gameId, Long userId) {
        String playerId = players.playerId(gameId, userId);
        if (playerId == null) throw denied();
        return playerId;
    }

    public void registerPlayers(
            String gameId,
            List<SessionMemberEntity> sessionMembers,
            List<PlayerStartInfo> gamePlayers
    ) {
        List<PlayerStartInfo> humanPlayers = gamePlayers.stream().filter(player -> !player.bot()).toList();
        if (humanPlayers.size() != sessionMembers.size()) {
            throw new IllegalStateException("authoritative player mapping mismatch");
        }
        var mapping = new LinkedHashMap<Long, String>();
        for (int i = 0; i < sessionMembers.size(); i++) {
            mapping.put(sessionMembers.get(i).getUserId(), humanPlayers.get(i).playerId());
        }
        players.put(gameId, mapping);
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("forbidden");
    }
}
