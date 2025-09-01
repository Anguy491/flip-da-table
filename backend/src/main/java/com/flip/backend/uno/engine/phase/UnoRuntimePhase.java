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
	// Stacking penalty state: if >0, the current player must either play same penalty type card to stack or draw total and lose turn
	private int pendingDrawPenalty = 0; // total cards to draw if player cannot / chooses not to stack
	private UnoCard.Type pendingPenaltyType = null; // DRAW_TWO or WILD_DRAW_FOUR while stacking
	private String pendingColorChooserPlayerId; // if set, waiting for this human to choose wild color
	private java.util.function.Consumer<UnoRuntimePhase> turnListener; // callback after each turn advance
	// --- Action log (Stage 1) ---
	public record ActionLogEntry(long seq, long turnId, String type, String actorId, String text, long ts) {}
	private final java.util.Deque<ActionLogEntry> actionLog = new java.util.ArrayDeque<>();
	private long nextSeq = 1L;
	private static final int LOG_CAPACITY = 50;
	private void addLog(String type, String actorId, String text) {
		long seq = nextSeq++;
		actionLog.addLast(new ActionLogEntry(seq, board.turnCount(), type, actorId, text, System.currentTimeMillis()));
		while (actionLog.size() > LOG_CAPACITY) actionLog.removeFirst();
	}
	public java.util.List<ActionLogEntry> actionLogSnapshot() { return java.util.List.copyOf(actionLog); }
	public long lastEventSeq() { return actionLog.isEmpty() ? 0L : actionLog.getLast().seq(); }
	public void setTurnListener(java.util.function.Consumer<UnoRuntimePhase> l) { this.turnListener = l; }
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
		// If under stacking penalty: bot either stacks or takes penalty; no other options
		if (pendingDrawPenalty > 0) {
			UnoCard stackable = current.getHand().view().stream()
				.filter(c -> c.getType() == pendingPenaltyType)
				.findFirst().orElse(null);
			if (stackable != null) {
				queue.enqueue(new UnoPlayCardEvent(board, deck, current, stackable));
				processQueue();
				if (resolveWinnerIfAny(current)) return;
				advanceTurn();
				addLog("TURN", null, "Turn -> " + board.currentPlayer().getId());
				if (turnListener != null) turnListener.accept(this);
				return;
			} else {
				applyAccumulatedPenalty(current); // draws all, loses turn
				if (resolveWinnerIfAny(current)) return; // Unlikely after drawing, but safe
				advanceTurn();
				addLog("TURN", null, "Turn -> " + board.currentPlayer().getId());
				if (turnListener != null) turnListener.accept(this);
				return;
			}
		}
		planTurn(current);
		processQueue();
		if (resolveWinnerIfAny(current)) return;
		advanceTurn();
		addLog("TURN", null, "Turn -> " + board.currentPlayer().getId());
		if (turnListener != null) turnListener.accept(this);
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
		// If awaiting color selection, only that command type is allowed from the chooser
		if (pendingColorChooserPlayerId != null && !cmd.type().equals("CHOOSE_COLOR")) {
			// Allow only the chooser to act
			if (!pendingColorChooserPlayerId.equals(cmd.playerId())) {
				return new CommandResult(false, List.of(new CommandError("WAIT_COLOR","Awaiting color choice")), buildView(cmd.playerId()));
			}
			// Chooser attempted another action instead of choosing color
			return new CommandResult(false, List.of(new CommandError("MUST_CHOOSE_COLOR","You must choose a color for the wild")), buildView(cmd.playerId()));
		}
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
					// If there is an active stacking penalty, only allow playing same penalty type to stack (or any wild needing color choice?)
					if (pendingDrawPenalty > 0) {
						if (pendingPenaltyType == UnoCard.Type.DRAW_TWO && card.getType() != UnoCard.Type.DRAW_TWO) {
							return fail("MUST_STACK_OR_DRAW","Must stack another DRAW_TWO or draw penalty", cmd.playerId());
						}
						if (pendingPenaltyType == UnoCard.Type.WILD_DRAW_FOUR && card.getType() != UnoCard.Type.WILD_DRAW_FOUR) {
							return fail("MUST_STACK_OR_DRAW","Must stack another WILD_DRAW_FOUR or draw penalty", cmd.playerId());
						}
					}
					queue.enqueue(event);
					processQueue();
					// After processing, detect if a color selection is required (event sets flag for human wild plays)
					if (!current.isBot() && (card.getType()==UnoCard.Type.WILD || card.getType()==UnoCard.Type.WILD_DRAW_FOUR) && board.activeColor() == null) {
						pendingColorChooserPlayerId = current.getId();
						applied = true; endTurn = false; // wait for color
					} else { applied = true; endTurn = true; }
					// (stack accumulation now handled centrally in processQueue)
				}
				case "DRAW_CARD" -> {
					if (pendingDrawPenalty > 0) {
						// Player chooses / is forced to take accumulated penalty now.
						applyAccumulatedPenalty(current);
						applied = true; endTurn = true;
					} else {
						var draw = new UnoDrawCardEvent(deck, current);
						queue.enqueue(draw); processQueue(); applied = true; endTurn = true;
					}
				}
				case "CHOOSE_COLOR" -> {
					if (pendingColorChooserPlayerId == null) return fail("NO_PENDING","No color selection pending", cmd.playerId());
					if (!pendingColorChooserPlayerId.equals(cmd.playerId())) return fail("NOT_CHOOSER","Not your color choice", cmd.playerId());
					UnoCard.Color chosen;
					try { chosen = UnoCard.Color.valueOf(cmd.color().toUpperCase()); }
					catch (Exception e) { return fail("BAD_COLOR","Invalid color", cmd.playerId()); }
					if (chosen == UnoCard.Color.WILD) return fail("BAD_COLOR","Cannot choose WILD as color", cmd.playerId());
					board.setActiveColor(chosen);
					// Apply any delayed draw four penalty (already applied in event execute for cards) -> nothing extra now
					pendingColorChooserPlayerId = null; applied = true; endTurn = true;
					addLog("COLOR_CHOOSE", cmd.playerId(), cmd.playerId()+" chose "+chosen.name());
				}
				case "DECLARE_UNO" -> {
					return new CommandResult(false, List.of(new CommandError("UNSUPPORTED","Command not implemented")), buildView(cmd.playerId()));
				}
				default -> { return new CommandResult(false, List.of(new CommandError("UNKNOWN_TYPE","Unknown command type")), buildView(cmd.playerId())); }
			}
			if (resolveWinnerIfAny(current)) {
				return new CommandResult(applied, List.copyOf(errors), buildView(cmd.playerId()));
			}
			if (endTurn) {
				advanceTurn();
				// If next player is under accumulation (pendingDrawPenalty>0) and is a bot, auto-resolution logic: try stacking else draw all.
				UnoPlayer next = (UnoPlayer) board.currentPlayer();
				addLog("TURN", null, "Turn -> " + next.getId());
				if (turnListener != null) turnListener.accept(this);
					while (winnerId == null && board.currentPlayer() instanceof UnoBot) { runBotTurn(); }
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
			// Require both number and color to match (color provided by client for disambiguation)
			if (!value.equals(String.valueOf(c.getNumber()))) return false;
			if (color == null) return true; // fallback (shouldn't happen)
			return c.getColor().name().equalsIgnoreCase(color);
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

	public boolean isAwaitingColorChoice() { return pendingColorChooserPlayerId != null; }
	public String awaitingColorChooser() { return pendingColorChooserPlayerId; }

	private void planTurn(UnoPlayer player) {
		UnoCard top = board.lastPlayedCard();
		UnoCard.Color active = board.activeColor();
		UnoCard playable = player.getHand().view().stream().filter(c -> canPlay(c, top, active)).findFirst().orElse(null);
		if (playable != null) {
			queue.enqueue(new UnoPlayCardEvent(board, deck, player, playable));
		} else {
			UnoDrawCardEvent draw = new UnoDrawCardEvent(deck, player);
			queue.enqueue(draw);
			// Rule variant: drawing a card always ends the turn; no immediate auto-play even if playable
		}
	}

	private void processQueue() {
		while (!queue.isEmpty()) {
			var e = queue.poll();
			if (e.isValid()) {
				e.execute();
				// If it's a draw, log only. Drawn card cannot be played immediately this turn per house rule.
				if (e instanceof UnoDrawCardEvent d) {
					UnoPlayer player = (UnoPlayer) d.source();
					addLog("DRAW", player.getId(), player.getId()+" drew 1 card");
				} else if (e instanceof UnoPlayCardEvent pce) {
					pendingAdvanceSteps = pce.getAdvanceSteps();
					UnoCard card = board.lastPlayedCard();
					UnoPlayer player = pce.getPlayer();
					String disp = card != null ? card.getDisplay() : "?";
					boolean wildType = card!=null && (card.getType()==UnoCard.Type.WILD || card.getType()==UnoCard.Type.WILD_DRAW_FOUR);
					addLog("PLAY", player.getId(), player.getId()+" played "+disp + (wildType?" (color "+(board.activeColor()!=null?board.activeColor().name():"?")+")":""));
					// Accumulate penalty if applicable
					if (pce.getPenaltyAmount() > 0) {
						if (pendingDrawPenalty == 0 && card != null) pendingPenaltyType = card.getType();
						pendingDrawPenalty += pce.getPenaltyAmount();
						addLog("PENALTY_STACK", player.getId(), player.getId()+" stacked "+pce.getPenaltyAmount()+" (total "+pendingDrawPenalty+")");
					}
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

	public int pendingDrawPenalty() { return pendingDrawPenalty; }
	public UnoCard.Type pendingPenaltyType() { return pendingPenaltyType; }

	/** Apply and clear any accumulated draw penalty to the provided player. */
	private void applyAccumulatedPenalty(UnoPlayer target) {
		if (pendingDrawPenalty <= 0) return;
		for (int i=0;i<pendingDrawPenalty;i++) {
			var card = deck.draw();
			if (card != null) target.giveCard(card);
		}
		addLog("PENALTY_DRAW", target.getId(), target.getId()+" drew "+pendingDrawPenalty+" cards (penalty)");
		pendingDrawPenalty = 0;
		pendingPenaltyType = null;
	}
}
