package com.flip.backend.dvc.engine.phase;

import com.flip.backend.game.engine.phase.EndingPhase;

/** Simple ending phase: prints winner id (may be null if draw). */
public class DVCEndingPhase extends EndingPhase {
	private final String winnerId;

	public DVCEndingPhase(String winnerId) { this.winnerId = winnerId; }

	public String winnerId() { return winnerId; }

	@Override
	public void enter() {
		System.out.println("DVC Game Over. Winner: " + (winnerId != null ? winnerId : "<none>"));
	}
}
