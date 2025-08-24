package com.flip.backend.game.service;

import com.flip.backend.game.engine.*;
import com.flip.backend.game.uno.UnoView;
import com.flip.backend.game.uno.command.UnoCommand;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** Generic runtime facade delegating to per-game runtime delegates. Currently wraps UNO only. */
@Service
public class GameRuntimeService {
    private final GameRepository games;
    private final java.util.Map<GameType, GameRuntimeDelegate> delegateMap = new java.util.EnumMap<>(GameType.class);
    private final UnoRuntimeDelegate unoDelegate; // kept for typed UNO methods

    public GameRuntimeService(GameRepository games, java.util.List<GameRuntimeDelegate> delegates, UnoRuntimeDelegate unoDelegate) {
        this.games = games;
        for (GameRuntimeDelegate d : delegates) delegateMap.put(d.supports(), d);
        this.unoDelegate = unoDelegate;
    }

    // UNO typed convenience (legacy front-end)
    @Transactional
    public CommandResult<?, ?> handleUnoCommand(String gameId, UnoCommand command) {
        GameEntity e = games.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found"));
        GameType type = GameType.valueOf(e.getGameType());
        if (type != GameType.UNO) throw new IllegalStateException("Not UNO game");
        return unoDelegate.handle(e, command);
    }

    @Transactional(readOnly = true)
    public UnoView getUnoView(String gameId, String viewerId) {
        GameEntity e = games.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found"));
        GameType type = GameType.valueOf(e.getGameType());
        if (type != GameType.UNO) throw new IllegalStateException("Not UNO game");
        return unoDelegate.getView(e, viewerId);
    }
}
