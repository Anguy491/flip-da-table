package com.flip.backend.game.uno;

import com.flip.backend.game.engine.StateProjector;

import java.util.ArrayList;
import java.util.List;

public class UnoStateProjector implements StateProjector<UnoState, UnoView> {
    @Override
    public UnoView toView(UnoState state, String viewerId) {
        List<UnoPlayerView> views = new ArrayList<>();
        for (int i=0;i<state.players.size();i++) {
            UnoPlayer p = state.players.get(i);
            boolean isCurrent = i == state.currentPlayerIndex;
            boolean isWinner = state.winners.contains(p.id());
            List<UnoCard> hand = p.id().equals(viewerId) ? List.copyOf(p.hand()) : List.of();
            views.add(new UnoPlayerView(p.id(), p.hand().size(), hand, isCurrent, isWinner));
        }
        return new UnoView(viewerId, views, state.pendingDraw, state.mustChooseColor, state.top(), state.phase);
    }
}
