package com.flip.backend.api;

import com.flip.backend.dvc.engine.DVCGameRegistry;
import com.flip.backend.dvc.engine.DVCStartRegistry;
import com.flip.backend.dvc.engine.phase.DVCRuntimePhase;
import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import com.flip.backend.dvc.engine.view.DVCView;
import com.flip.backend.service.game.DVCGameService;
import org.springframework.web.bind.annotation.*;

/** Minimal REST controller for DVC interactions (prototype). */
@RestController
@RequestMapping("/api/dvc")
public class DVCController {
	private final DVCGameRegistry runtimeRegistry;
	private final DVCStartRegistry startRegistry;
	public DVCController(DVCGameRegistry runtimeRegistry, DVCStartRegistry startRegistry, DVCGameService gameService) {
		this.runtimeRegistry = runtimeRegistry; this.startRegistry = startRegistry; }

	private DVCRuntimePhase runtime(String gameId) { return runtimeRegistry.get(gameId); }
	private DVCStartPhase startPhase(String gameId) { return startRegistry.get(gameId); }

	@GetMapping("/{gameId}/view/{playerId}")
	public DVCView view(@PathVariable String gameId, @PathVariable String playerId) {
		var rt = runtime(gameId); if (rt!=null) return rt.buildView(playerId); var sp = startPhase(gameId); return sp==null?null: sp.buildView(playerId);
	}

	public record DrawColorRequest(String playerId, String color) {}
	@PostMapping("/{gameId}/drawColor")
	public boolean drawColor(@PathVariable String gameId, @RequestBody DrawColorRequest req) {
		var rt = runtime(gameId); if (rt==null) return false; return rt.provideDrawColor(req.playerId(), req.color());
	}

	public record GuessRequest(String playerId, String targetPlayerId, int targetIndex, boolean joker, Integer number) {}
	@PostMapping("/{gameId}/guess")
	public boolean guess(@PathVariable String gameId, @RequestBody GuessRequest req) {
		var rt = runtime(gameId); if (rt==null) return false; return rt.provideGuess(req.playerId(), req.targetPlayerId(), req.targetIndex(), req.joker(), req.number());
	}

	public record RevealDecisionRequest(String playerId, boolean cont) {}
	@PostMapping("/{gameId}/revealDecision")
	public boolean revealDecision(@PathVariable String gameId, @RequestBody RevealDecisionRequest req) {
		var rt = runtime(gameId); if (rt==null) return false; return rt.provideRevealDecision(req.playerId(), req.cont());
	}

	public record SelfRevealRequest(String playerId, int ownIndex) {}
	@PostMapping("/{gameId}/selfReveal")
	public boolean selfReveal(@PathVariable String gameId, @RequestBody SelfRevealRequest req) {
		var rt = runtime(gameId); if (rt==null) return false; return rt.provideSelfReveal(req.playerId(), req.ownIndex());
	}

	public record SettleRequest(String playerId, Boolean isSettled, String hand) {}
	@PostMapping("/{gameId}/settle")
	public boolean settle(@PathVariable String gameId, @RequestBody SettleRequest req) {
		// If still in start phase interpret as initial arrange + settle
		var sp = startPhase(gameId);
		if (sp != null) {
			if (req.hand()!=null) sp.reorderHand(req.playerId(), req.hand());
			if (Boolean.TRUE.equals(req.isSettled())) sp.settled(req.playerId());
			// Auto transit when all settled
			if (sp.allSettled()) {
				var runtime = sp.transit();
				runtime.enter();
				startRegistry.remove(gameId);
				runtimeRegistry.put(gameId, runtime);
			}
			return true;
		}
		var rt = runtime(gameId); if (rt==null) return false; return rt.provideSettleHand(req.playerId(), req.hand());
	}
}
