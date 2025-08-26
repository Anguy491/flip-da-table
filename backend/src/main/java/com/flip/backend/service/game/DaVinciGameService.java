package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class DaVinciGameService extends GameService {
    public DaVinciGameService(SessionRepository sessions, GameRepository games) { super(sessions, games); }

    @Override public boolean supports(String gameType) { return "DAVINCI".equalsIgnoreCase(gameType); }

    @Override
    public StartGameResponse startFirst(String sessionId, StartGameRequest req) {
        var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
        if (!supports(session.getGameType())) throw new IllegalArgumentException("Unsupported game type for DaVinci service");
        int players = countValidPlayers(req);
        if (players < 2 || players > 4) throw new IllegalArgumentException("players must be 2-4 for DaVinci");
        return persistRound(session, 1);
    }
}
