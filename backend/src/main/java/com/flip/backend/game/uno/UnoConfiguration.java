package com.flip.backend.game.uno;

import com.flip.backend.game.engine.GameType;
import com.flip.backend.game.service.GameDefinition;
import com.flip.backend.game.service.GameEngineService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/** Registers UNO engine + projector with GameService at startup. */
@Configuration
public class UnoConfiguration {
    private final GameEngineService gameService;

    public UnoConfiguration(GameEngineService gameService) {
        this.gameService = gameService;
    }

    @PostConstruct
    public void register() {
        gameService.register(new GameDefinition<>(
                GameType.UNO,
                UnoState.class,
                new UnoEngine(),
                new UnoStateProjector()
        ));
    }
}
