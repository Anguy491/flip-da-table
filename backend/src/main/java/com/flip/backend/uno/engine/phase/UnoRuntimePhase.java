package com.flip.backend.uno.engine.phase;

import com.flip.backend.game.engine.phase.RuntimePhase;
import com.flip.backend.uno.entities.*;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.uno.engine.event.*;
import com.flip.backend.uno.engine.view.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/** Prototype UNO runtime loop without special card effects. */
public class UnoRuntimePhase extends RuntimePhase {
	private final UnoDeck deck;
	private final UnoBoard board;
	private final EventQueue queue = new EventQueue();
	private String winnerId;
	private UnoEndingPhase endingPhase; // populated when winner decided
	private int pendingAdvanceSteps = 1;

	public UnoRuntimePhase(UnoDeck deck, UnoBoard board, List<UnoPlayer> players) {
		this.deck = deck; this.board = board; // players list not needed for now
	}

	@Override public void enter() { /* nothing extra for now */ }

	@Override
	public String run() {
		while (winnerId == null) {
			runSingleTurn(); // bot simulation loop for tests
		}
		return winnerId;
	}

	/** Execute exactly one player's full automated turn (bot). */
	public void runSingleTurn() { if (winnerId == null) runBotTurn(); }

	private void runBotTurn() {
		UnoPlayer current = (UnoPlayer) board.currentPlayer();
		planTurn(current);
		processQueue();
		if (resolveWinnerIfAny(current)) return;
		advanceTurn();
	}

	private boolean resolveWinnerIfAny(UnoPlayer current) {
		if (current.cardCount() == 0) { winnerId = current.getId(); endingPhase = new UnoEndingPhase(current); return true; }
		return false;
	}

	private void advanceTurn() {
		board.step(pendingAdvanceSteps); board.tickTurn(); pendingAdvanceSteps = 1; }

	/** Player command DTO (internal). */
	public record PlayerCommand(String type, String playerId, String color, String value) {
		public PlayerCommand { type = Objects.requireNonNullElse(type, "").toUpperCase(); }
	}

	public record CommandError(String code, String message) {}
	public record CommandResult(boolean applied, List<CommandError> errors, UnoView view) {}

	/** Apply a human player's command in an event-driven style (thread-unsafe outside). */
	public synchronized CommandResult applyPlayerCommand(PlayerCommand cmd) {
		List<CommandError> errors = new ArrayList<>();
		if (cmd == null || cmd.playerId() == null) return new CommandResult(false, List.of(new CommandError("BAD_REQUEST","null command")), buildView(null));
		if (winnerId != null) return new CommandResult(false, List.of(new CommandError("FINISHED","Game already finished")), buildView(cmd.playerId()));
		UnoPlayer current = (UnoPlayer) board.currentPlayer();
		if (!current.getId().equals(cmd.playerId())) {
			return new CommandResult(false, List.of(new CommandError("NOT_TURN","Not your turn")), buildView(cmd.playerId()));
		}
		boolean endTurn = false;
		boolean applied = false;
		try {
			switch (cmd.type()) {
				case "PLAY_CARD" -> {
					UnoCard card = findCard(current, cmd);
					if (card == null) return fail("CARD_NOT_FOUND","Card not in hand", cmd.playerId());
					var event = new UnoPlayCardEvent(board, deck, current, card);
					if (!event.isValid()) return fail("ILLEGAL_PLAY","Card not playable", cmd.playerId());
					queue.enqueue(event);
					processQueue();
					applied = true; endTurn = true;
				}
				case "DRAW_CARD" -> {
					var draw = new UnoDrawCardEvent(deck, current);
					queue.enqueue(draw); processQueue(); applied = true; endTurn = true;
				}
				case "CHOOSE_COLOR", "DECLARE_UNO" -> {
					// Not implemented yet
					return new CommandResult(false, List.of(new CommandError("UNSUPPORTED","Command not implemented")), buildView(cmd.playerId()));
				}
				default -> { return new CommandResult(false, List.of(new CommandError("UNKNOWN_TYPE","Unknown command type")), buildView(cmd.playerId())); }
			}
			if (resolveWinnerIfAny(current)) {
				return new CommandResult(applied, List.copyOf(errors), buildView(cmd.playerId()));
			}
			if (endTurn) {
				advanceTurn();
				// Auto-run consecutive bot turns
				while (winnerId == null && board.currentPlayer() instanceof UnoBot) {
					runBotTurn();
				}
			}
			return new CommandResult(applied, List.copyOf(errors), buildView(cmd.playerId()));
		} catch (Exception ex) {
			errors.add(new CommandError("EXCEPTION", ex.getMessage()));
			return new CommandResult(false, List.copyOf(errors), buildView(cmd.playerId()));
		}
	}

	private CommandResult fail(String code, String msg, String perspective) {
		return new CommandResult(false, List.of(new CommandError(code, msg)), buildView(perspective));
	}

	private UnoCard findCard(UnoPlayer player, PlayerCommand cmd) {
		if (cmd.value() == null) return null;
		String val = cmd.value();
		for (UnoCard c : player.getHand().view()) {
			if (match(c, cmd.color(), val)) return c;
		}
		return null;
	}

	private boolean match(UnoCard c, String color, String value) {
		if (c.getType() == UnoCard.Type.NUMBER) {
			return value.equals(String.valueOf(c.getNumber()));
		}
		switch (value) {
			case "SKIP" -> { return c.getType()== UnoCard.Type.SKIP && colorEquals(c, color); }
			case "REVERSE" -> { return c.getType()== UnoCard.Type.REVERSE && colorEquals(c, color); }
			case "DRAW_TWO" -> { return c.getType()== UnoCard.Type.DRAW_TWO && colorEquals(c, color); }
			case "WILD" -> { return c.getType()== UnoCard.Type.WILD; }
			case "WILD_DRAW_FOUR" -> { return c.getType()== UnoCard.Type.WILD_DRAW_FOUR; }
		}
		return false;
	}

	private boolean colorEquals(UnoCard c, String color) {
		if (color == null) return false;
		return c.getColor().name().equalsIgnoreCase(color);
	}

	public UnoEndingPhase endingPhase() { return endingPhase; }

	/** Build a snapshot view for the given player id (full hand for self, counts for others). */
	public UnoView buildView(String perspectivePlayerId) {
		// Determine current player index by traversing snapshotOrder
		var order = board.snapshotOrder();
		int currentIndex = 0;
		for (int i=0;i<order.size();i++) if (order.get(i).getId().equals(board.currentPlayer().getId())) { currentIndex = i; break; }
		UnoBoardView boardView = new UnoBoardView(
			"UNO",
			board.turnCount(),
			board.direction(),
			currentIndex,
			board.lastPlayedCard() != null ? board.lastPlayedCard().getDisplay() : null,
			board.activeColor() != null ? board.activeColor().name() : null,
			deck.remainingDraw(),
			deck.discardSize()
		);
		java.util.List<UnoPlayerView> playerViews = new java.util.ArrayList<>();
		for (var p : order) {
			boolean self = p.getId().equals(perspectivePlayerId);
			var hand = p.getHand();
			java.util.List<String> handDisplays = self ? hand.view().stream().map(UnoCard::getDisplay).toList() : null;
			playerViews.add(new UnoPlayerView(p.getId(), p.isBot(), hand.size(), handDisplays));
		}
		return new UnoView(boardView, java.util.List.copyOf(playerViews), perspectivePlayerId);
	}

	private void planTurn(UnoPlayer player) {
		UnoCard top = board.lastPlayedCard();
		UnoCard.Color active = board.activeColor();
		UnoCard playable = player.getHand().view().stream().filter(c -> canPlay(c, top, active)).findFirst().orElse(null);
		if (playable != null) {
			queue.enqueue(new UnoPlayCardEvent(board, deck, player, playable));
		} else {
			UnoDrawCardEvent draw = new UnoDrawCardEvent(deck, player);
			queue.enqueue(draw);
			// After draw we may attempt play of that single card (lazy decision in processing after draw)
		}
	}

	private void processQueue() {
		while (!queue.isEmpty()) {
			var e = queue.poll();
			if (e.isValid()) {
				e.execute();
				// If it's a draw, see if drawn is playable -> enqueue play event
				if (e instanceof UnoDrawCardEvent d) {
					UnoCard drawn = d.drawnCard();
					UnoPlayer player = (UnoPlayer) d.source();
					if (drawn != null && canPlay(drawn, board.lastPlayedCard(), board.activeColor())) {
						queue.enqueue(new UnoPlayCardEvent(board, deck, player, drawn));
					}
				} else if (e instanceof UnoPlayCardEvent pce) {
					pendingAdvanceSteps = pce.getAdvanceSteps();
				}
			}
		}
	}

	private boolean canPlay(UnoCard card, UnoCard top, UnoCard.Color activeColor) {
		if (card.getType() == UnoCard.Type.WILD || card.getType() == UnoCard.Type.WILD_DRAW_FOUR) return true; // simplified
		if (top == null) return true;
		if (card.getColor() != UnoCard.Color.WILD && card.getColor() == activeColor) return true;
		if (card.getType() == top.getType() && card.getType() != UnoCard.Type.NUMBER) return true;
		return card.getType() == UnoCard.Type.NUMBER && top.getType() == UnoCard.Type.NUMBER && card.getNumber().equals(top.getNumber());
	}
}
