package com.flip.backend.game.service;

import com.flip.backend.game.engine.*;
import com.flip.backend.game.uno.*;
import com.flip.backend.game.uno.command.UnoCommand;
import com.flip.backend.game.uno.event.UnoEvent;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UNO-specific runtime logic (command handling + bot autoplay + view projection). */
@Component
public class UnoRuntimeDelegate implements GameRuntimeDelegate {
    private final GameRepository games;
    private final GameStateSerializer serializer;
    private final GameDefinitionRegistry registry;

    public UnoRuntimeDelegate(GameRepository games, GameStateSerializer serializer) {
        this.games = games;
        this.serializer = serializer;
        this.registry = new GameDefinitionRegistry();
        registry.register(new GameDefinition<>(
                GameType.UNO,
                UnoState.class,
                new UnoEngine(),
                new UnoStateProjector()
        ));
    }

    @Override
    public GameType supports() { return GameType.UNO; }

    @Transactional
    public CommandResult<UnoState, UnoEvent> handle(GameEntity e, UnoCommand command) {
        UnoState state = serializer.deserialize(e.getStateJson(), UnoState.class);
        @SuppressWarnings({"rawtypes","unchecked"})
        GameEngine<UnoState, UnoCommand, UnoEvent> engine = (GameEngine) registry.get(GameType.UNO).engine();
        var vr = engine.validate(state, command);
        if (!vr.isOk()) {
            return new CommandResult<>(false, state, List.of(), vr);
        }
        List<UnoEvent> events = engine.decide(state, command);
        UnoState newState = state;
        for (UnoEvent ev : events) newState = engine.apply(newState, ev);
        e.setStateJson(serializer.serialize(newState));
        games.save(e);
        autoPlayBots(e, engine); // chain bot turns
        UnoState finalState = serializer.deserialize(e.getStateJson(), UnoState.class);
        return new CommandResult<>(true, finalState, events, vr);
    }

    @Transactional(readOnly = true)
    public UnoView getView(GameEntity e, String viewerId) {
        UnoState state = serializer.deserialize(e.getStateJson(), UnoState.class);
        @SuppressWarnings({"rawtypes","unchecked"})
        StateProjector<UnoState, UnoView> projector = (StateProjector) registry.get(GameType.UNO).projector();
        return projector.toView(state, viewerId);
    }

    @Transactional
    protected void autoPlayBots(GameEntity entity, GameEngine<UnoState, UnoCommand, UnoEvent> engine) {
        UnoState state = serializer.deserialize(entity.getStateJson(), UnoState.class);
        boolean changed = false;
        while (!engine.isTerminal(state)) {
            UnoPlayer current = state.currentPlayer();
            if (!current.bot()) break;
            if (state.mustChooseColor) {
                UnoColor best = pickBestColor(current);
                UnoCommand choose = new com.flip.backend.game.uno.command.ChooseColor(current.id(), best);
                var vr = engine.validate(state, choose);
                if (!vr.isOk()) break;
                List<UnoEvent> evs = engine.decide(state, choose);
                for (UnoEvent ev : evs) state = engine.apply(state, ev);
                changed = true;
                continue;
            }
            UnoCommand action = pickBotAction(state, current);
            if (action == null) action = new com.flip.backend.game.uno.command.DrawCard(current.id());
            var vr = engine.validate(state, action);
            if (!vr.isOk()) break;
            List<UnoEvent> evs = engine.decide(state, action);
            for (UnoEvent ev : evs) state = engine.apply(state, ev);
            changed = true;
        }
        if (changed) {
            entity.setStateJson(serializer.serialize(state));
            games.save(entity);
        }
    }

    private UnoColor pickBestColor(UnoPlayer player) {
        int r=0,g=0,b=0,y=0;
        for (UnoCard c: player.hand()) {
            if (c.color()==null) continue;
            switch (c.color()) {
                case RED -> r++;
                case GREEN -> g++;
                case BLUE -> b++;
                case YELLOW -> y++;
            }
        }
        int max = Math.max(Math.max(r,g), Math.max(b,y));
        if (r==max) return UnoColor.RED;
        if (g==max) return UnoColor.GREEN;
        if (b==max) return UnoColor.BLUE;
        return UnoColor.YELLOW;
    }

    private UnoCommand pickBotAction(UnoState state, UnoPlayer current) {
        UnoCard top = state.top();
        if (state.pendingDraw>0) {
            for (UnoCard c : current.hand()) {
                if (c.value()==UnoValue.DRAW_TWO || c.value()==UnoValue.WILD_DRAW_FOUR) {
                    return new com.flip.backend.game.uno.command.PlayCard(current.id(), c.color(), c.value());
                }
            }
        }
        for (UnoCard c : current.hand()) {
            boolean matches = c.value()==top.value() || (c.color()!=null && c.color()==top.color()) || c.isWild();
            if (matches) return new com.flip.backend.game.uno.command.PlayCard(current.id(), c.color(), c.value());
        }
        return null;
    }
}
