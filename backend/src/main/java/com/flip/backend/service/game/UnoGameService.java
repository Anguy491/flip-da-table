package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class UnoGameService extends GameService {
	public UnoGameService(SessionRepository sessions, GameRepository games) { super(sessions, games); }

	@Override public boolean supports(String gameType) { return "UNO".equalsIgnoreCase(gameType); }

	@Override
	public StartGameResponse startFirst(String sessionId, StartGameRequest req) {
		var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
		if (!supports(session.getGameType())) throw new IllegalArgumentException("Unsupported game type for UNO service");
		int players = countValidPlayers(req);
		if (players < 2) throw new IllegalArgumentException("UNO requires at least 2 players");
		// UNO 简化：第一局 roundIndex=1
		return persistRound(session, 1);
	}
}
