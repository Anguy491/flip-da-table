package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.uno.engine.phase.UnoStartPhase;
import com.flip.backend.uno.engine.phase.UnoRuntimePhase;
import com.flip.backend.uno.engine.UnoGameRegistry;
import org.springframework.stereotype.Service;

@Service
public class UnoGameService extends GameService {
	private final UnoGameRegistry registry;
	public UnoGameService(SessionRepository sessions, GameRepository games, UnoGameRegistry registry) { super(sessions, games); this.registry = registry; }

	@Override public boolean supports(String gameType) { return "UNO".equalsIgnoreCase(gameType); }

	@Override
	public StartGameResponse startFirst(String sessionId, StartGameRequest req) {
		var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
		if (!supports(session.getGameType())) throw new IllegalArgumentException("Unsupported game type for UNO service");
		int players = countValidPlayers(req);
		if (players < 2) throw new IllegalArgumentException("UNO requires at least 2 players");

		var base = persistRound(session, 1);

		// Build ordered player ids (simple deterministic). We'll transform provided names -> id tokens.
		java.util.List<PlayerStartInfo> playerInfos = new java.util.ArrayList<>();
		java.util.List<String> playerIds = new java.util.ArrayList<>();
		int idx = 1;
		for (var spec : req.players()) {
			if (spec.name()==null || spec.name().isBlank()) continue; // skip blanks
			String raw = spec.name().trim();
			String id = (spec.bot() ? "BOT" : "P") + idx + "_" + raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
			playerIds.add(id);
			playerInfos.add(new PlayerStartInfo(id, raw, spec.bot(), spec.ready()));
			idx++;
		}
		// Host perspective: first non-bot or fallback first
		String myPlayerId = playerInfos.stream().filter(p -> !p.bot()).map(PlayerStartInfo::playerId).findFirst()
			.orElse(playerInfos.isEmpty()?null:playerInfos.get(0).playerId());

		// Initialize UNO start phase -> runtime and build initial view for host.
		UnoStartPhase startPhase = new UnoStartPhase(playerIds);
		startPhase.enter();
		UnoRuntimePhase runtime = startPhase.transit();
		registry.put(base.gameId(), runtime);
		var view = runtime.buildView(myPlayerId); // initial snapshot
		return new StartGameResponse(base.gameId(), base.roundIndex(), myPlayerId, java.util.List.copyOf(playerInfos), view);
	}
}
