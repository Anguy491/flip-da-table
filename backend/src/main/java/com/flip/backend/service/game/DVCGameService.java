package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.dvc.engine.DVCGameRegistry;
import com.flip.backend.dvc.engine.DVCStartRegistry;
import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DVCGameService extends GameService {
    private final DVCGameRegistry runtimeRegistry;
    private final DVCStartRegistry startRegistry;
    public DVCGameService(SessionRepository sessions, GameRepository games, DVCGameRegistry runtimeRegistry, DVCStartRegistry startRegistry) {
        super(sessions, games);
        this.runtimeRegistry = runtimeRegistry;
        this.startRegistry = startRegistry;
    }

    @Override public boolean supports(String gameType) { return "DAVINCI".equalsIgnoreCase(gameType); }

    @Override
    protected boolean isFinished(String gameId) {
        var runtime = runtimeRegistry.get(gameId);
        return runtime != null && runtime.isFinished();
    }

    @Override
    public Object viewFor(String gameId, String playerId) {
        var runtime = runtimeRegistry.get(gameId);
        if (runtime != null) return runtime.buildView(playerId);
        var start = startRegistry.get(gameId);
        if (start != null) return start.buildView(playerId);
        throw new IllegalArgumentException("game not found");
    }

    @Override
    @Transactional
    public StartGameResponse startFirst(String sessionId, StartGameRequest req) {
        var session = beginFirstRound(sessionId);
        if (!supports(session.getGameType())) throw new IllegalArgumentException("Unsupported game type for DaVinci service");
        int players = countValidPlayers(req);
        if (players < 2 || players > 4) throw new IllegalArgumentException("players must be 2-4 for DaVinci");
        var base = persistRound(session, 1);

        // Build player ids & infos (reuse UNO pattern)
        java.util.List<PlayerStartInfo> playerInfos = new java.util.ArrayList<>();
        java.util.List<String> playerIds = new java.util.ArrayList<>();
        int seq = 1; int botSeq = 1;
        for (var spec : req.players()) {
            if (spec.name()==null || spec.name().isBlank()) continue;
            String raw = spec.name().trim();
            String sanitized = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            String id;
            if (spec.bot()) id = "BOT" + botSeq++;
            else id = "P" + (seq++) + "_" + sanitized;
            playerIds.add(id);
            playerInfos.add(new PlayerStartInfo(id, raw, spec.bot(), spec.ready()));
        }
        String myPlayerId = playerInfos.stream().filter(p->!p.bot()).map(PlayerStartInfo::playerId).findFirst()
            .orElse(playerInfos.isEmpty()?null:playerInfos.get(0).playerId());

        // Start phase with manual ready concept. For MVP we auto-ready all (could expose API later)
    DVCStartPhase startPhase = new DVCStartPhase(playerIds);
    startPhase.enter();
    startRegistry.put(base.gameId(), startPhase);
    var view = startPhase.buildView(myPlayerId);
        return new StartGameResponse(base.gameId(), base.roundIndex(), myPlayerId, java.util.List.copyOf(playerInfos), view);
    }

    @Override
    @Transactional
    public StartGameResponse startNext(String sessionId, StartGameRequest req) {
        var nextRound = beginNextRound(sessionId);
        var session = nextRound.session();
        if (!supports(session.getGameType())) throw new IllegalArgumentException("Unsupported game type for DaVinci service");
        int players = countValidPlayers(req);
        if (players < 2 || players > 4) throw new IllegalArgumentException("players must be 2-4 for DaVinci");
        var base = persistRound(session, nextRound.roundIndex());
        java.util.List<PlayerStartInfo> playerInfos = new java.util.ArrayList<>();
        java.util.List<String> playerIds = new java.util.ArrayList<>();
        int seq = 1; int botSeq = 1;
        for (var spec : req.players()) {
            if (spec.name()==null || spec.name().isBlank()) continue;
            String raw = spec.name().trim();
            String sanitized = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            String id = spec.bot()?"BOT" + botSeq++:"P" + (seq++) + "_" + sanitized;
            playerIds.add(id);
            playerInfos.add(new PlayerStartInfo(id, raw, spec.bot(), spec.ready()));
        }
        String myPlayerId = playerInfos.stream().filter(p->!p.bot()).map(PlayerStartInfo::playerId).findFirst()
            .orElse(playerInfos.isEmpty()?null:playerInfos.get(0).playerId());
        DVCStartPhase startPhase = new DVCStartPhase(playerIds);
        startPhase.enter();
        startRegistry.put(base.gameId(), startPhase);
        var view = startPhase.buildView(myPlayerId);
		return new StartGameResponse(base.gameId(), base.roundIndex(), myPlayerId, java.util.List.copyOf(playerInfos), view);
    }
}
