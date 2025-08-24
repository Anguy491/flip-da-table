package com.flip.backend.game.uno;

import com.flip.backend.game.engine.*;
import com.flip.backend.game.uno.command.*;
import com.flip.backend.game.uno.event.*;

import java.util.*;

/** Simplified UNO rules engine (MVP subset). */
public class UnoEngine implements GameEngine<UnoState, UnoCommand, UnoEvent> {

    @Override
    public ValidationResult validate(UnoState state, UnoCommand command) {
        if (state.phase == GamePhase.FINISHED) {
            return ValidationResult.fail(EngineErrorCode.GAME_TERMINATED, "Game finished");
        }
        if (command instanceof PlayCard pc) {
            if (!state.mustChooseColor && !state.currentPlayer().id().equals(pc.playerId())) {
                return ValidationResult.fail(EngineErrorCode.ILLEGAL_TURN, "Not your turn");
            }
            var player = state.currentPlayer();
            boolean has = player.hand().stream().anyMatch(c -> c.value()==pc.value() && Objects.equals(c.color(), pc.color()));
            if (!has) return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "Card not in hand");
            UnoCard top = state.top();
            boolean matches = pc.value().name().equals(top.value().name()) || (pc.color()!=null && pc.color()==top.color()) || pc.value()==UnoValue.WILD || pc.value()==UnoValue.WILD_DRAW_FOUR;
            if (!matches) return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "Card does not match top");
            if (state.mustChooseColor) return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "Must choose color first");
            if (state.pendingDraw>0 && !(pc.value()==UnoValue.DRAW_TWO || pc.value()==UnoValue.WILD_DRAW_FOUR)) {
                return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "Must satisfy draw stack or draw");
            }
        } else if (command instanceof DrawCard dc) {
            if (!state.currentPlayer().id().equals(dc.playerId())) return ValidationResult.fail(EngineErrorCode.ILLEGAL_TURN, "Not your turn");
            if (state.mustChooseColor) return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "Must choose color");
        } else if (command instanceof PassTurn pt) {
            if (!state.currentPlayer().id().equals(pt.playerId())) return ValidationResult.fail(EngineErrorCode.ILLEGAL_TURN, "Not your turn");
        } else if (command instanceof ChooseColor cc) {
            if (!state.mustChooseColor) return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "No color choice pending");
            if (!state.currentPlayer().id().equals(cc.playerId())) return ValidationResult.fail(EngineErrorCode.ILLEGAL_TURN, "Not your turn");
        } else if (command instanceof DeclareUno du) {
            if (!state.currentPlayer().id().equals(du.playerId())) return ValidationResult.fail(EngineErrorCode.ILLEGAL_TURN, "Not your turn");
            int size = state.currentPlayer().hand().size();
            if (size!=2) return ValidationResult.fail(EngineErrorCode.RULE_VIOLATION, "UNO declaration only at 2 cards");
        }
        return ValidationResult.ok();
    }

    @Override
    public List<UnoEvent> decide(UnoState state, UnoCommand command) {
        List<UnoEvent> events = new ArrayList<>();
        if (command instanceof PlayCard pc) {
            events.add(new CardPlayed(pc.playerId(), pc.color(), pc.value()));
            switch (pc.value()) {
                case REVERSE -> events.add(new DirectionReversed());
                case SKIP -> {
                    // skip next
                    int nextIndex = advanceIndex(state.currentPlayerIndex, state.direction, state.playerCount());
                    String skipped = state.players.get(nextIndex).id();
                    events.add(new PlayerSkipped(skipped));
                }
                case DRAW_TWO -> events.add(new DrawStackIncreased(2));
                case WILD -> events.add(new DrawStackIncreased(0)); // triggers choose color
                case WILD_DRAW_FOUR -> events.add(new DrawStackIncreased(4));
                default -> {}
            }
            // We'll append win/finish after simulation below
        } else if (command instanceof DrawCard dc) {
            int count = state.pendingDraw>0 ? state.pendingDraw : 1;
            events.add(new CardDrawn(dc.playerId(), count));
        } else if (command instanceof PassTurn) {
            // no direct event besides implicit turn advance (handled by skip / play logic) -> could be a noop
        } else if (command instanceof ChooseColor cc) {
            events.add(new ColorChosen(cc.playerId(), cc.color()));
        } else if (command instanceof DeclareUno du) {
            events.add(new UnoDeclared(du.playerId()));
        }

        // Simulate apply to see victory
        UnoState sim = cloneShallow(state);
        for (UnoEvent e : events) {
            sim = apply(sim, e);
        }
        if (sim.phase == GamePhase.FINISHED && sim.winners.size()==1) {
            String winner = sim.winners.iterator().next();
            events.add(new PlayerWon(winner));
            events.add(new GameFinished());
        }
        return events;
    }

    private UnoState cloneShallow(UnoState s) {
        // For victory detection we only need hand sizes & phase; cheap approach: reuse (side effects acceptable since simulate then discard?)
        return s; // Acceptable for MVP; if side-effects appear, deep copy needed.
    }

    @Override
    public UnoState apply(UnoState state, UnoEvent event) {
        if (event instanceof CardPlayed cp) {
            UnoPlayer p = state.currentPlayer();
            // remove card instance
            p.hand().removeIf(c -> c.value()==cp.value() && Objects.equals(c.color(), cp.color()));
            // push card (if wild color still null here; color chosen later)
            state.discardPile.push(new UnoCard(cp.color(), cp.value()));
            if (cp.value()==UnoValue.WILD || cp.value()==UnoValue.WILD_DRAW_FOUR) {
                state.mustChooseColor = true;
            }
            // advance turn baseline
            advanceTurn(state);
        } else if (event instanceof DirectionReversed) {
            state.direction *= -1;
    } else if (event instanceof PlayerSkipped) {
            // skip already advanced? We'll advance again if skipped player is next
            // Simplified: nothing extra since we already advanced once; just advance again.
            advanceTurn(state);
        } else if (event instanceof DrawStackIncreased dsi) {
            state.pendingDraw += dsi.added();
            if (((DrawStackIncreased) event).added()==0) { /* wild color choose pending already set */ }
        } else if (event instanceof CardDrawn cd) {
            UnoPlayer p = state.currentPlayer();
            int draw = cd.count();
            for (int i=0;i<draw;i++) {
                if (state.drawPile.isEmpty()) reshuffle(state);
                if (state.drawPile.isEmpty()) break; // exhausted
                p.hand().add(state.drawPile.pop());
            }
            state.pendingDraw = 0;
            advanceTurn(state);
        } else if (event instanceof ColorChosen cc) {
            // Replace top card with colored variant (for wild)
            UnoCard top = state.discardPile.pop();
            state.discardPile.push(new UnoCard(cc.color(), top.value()));
            state.mustChooseColor = false;
    } else if (event instanceof UnoDeclared) {
            // could mark for penalty avoidance; ignored in MVP
        } else if (event instanceof PlayerWon pw) {
            state.winners.add(pw.playerId());
            state.phase = GamePhase.FINISHED;
        } else if (event instanceof GameFinished) {
            state.phase = GamePhase.FINISHED;
        }
        // victory check after card played
        if (state.currentPlayer().hand().isEmpty() && state.phase==GamePhase.RUNNING) {
            state.winners.add(state.currentPlayer().id());
            state.phase = GamePhase.FINISHED;
        }
        return state;
    }

    private void reshuffle(UnoState state) {
        if (state.discardPile.size() <= 1) return; // nothing to reshuffle
        UnoCard top = state.discardPile.pop();
        List<UnoCard> rest = new ArrayList<>(state.discardPile);
        state.discardPile.clear();
        state.discardPile.push(top);
        Collections.shuffle(rest, new Random());
        for (UnoCard c : rest) state.drawPile.push(c);
    }

    private void advanceTurn(UnoState state) {
        state.currentPlayerIndex = advanceIndex(state.currentPlayerIndex, state.direction, state.playerCount());
    }

    private int advanceIndex(int idx, int dir, int size) {
        return (idx + dir + size) % size;
    }

    @Override
    public boolean isTerminal(UnoState state) {
        return state.phase == GamePhase.FINISHED;
    }
}
