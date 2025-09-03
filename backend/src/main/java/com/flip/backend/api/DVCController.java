package com.flip.backend.api;

import com.flip.backend.dvc.engine.DVCGameRegistry;
import com.flip.backend.dvc.engine.phase.DVCRuntimePhase;
import com.flip.backend.dvc.engine.view.DVCView;
import org.springframework.web.bind.annotation.*;

/** Minimal REST controller for DVC interactions (prototype). */
@RestController
@RequestMapping("/api/dvc")
public class DVCController {
	private final DVCGameRegistry registry;
	public DVCController(DVCGameRegistry registry) { this.registry = registry; }

	private DVCRuntimePhase runtime(String gameId) { return registry.get(gameId); }

	@GetMapping("/{gameId}/view/{playerId}")
	public DVCView view(@PathVariable String gameId, @PathVariable String playerId) {
		var rt = runtime(gameId); if (rt==null) return null; return rt.buildView(playerId);
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

	public record SettleRequest(String playerId, Integer insertIndex) {}
	@PostMapping("/{gameId}/settle")
	public boolean settle(@PathVariable String gameId, @RequestBody SettleRequest req) {
		var rt = runtime(gameId); if (rt==null) return false; return rt.provideSettlePosition(req.playerId(), req.insertIndex());
	}
}
