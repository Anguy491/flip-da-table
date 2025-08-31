package com.flip.backend.api;

import com.flip.backend.uno.engine.UnoGameRegistry;
import com.flip.backend.uno.engine.phase.UnoRuntimePhase;
import com.flip.backend.uno.engine.view.UnoView;
import com.flip.backend.uno.engine.view.UnoPlayerView;
import com.flip.backend.uno.engine.view.UnoBoardView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;

@RestController
@RequestMapping("/api/games/uno")
public class UnoController {
    private final UnoGameRegistry registry;
    private final UnoSseService sseService;
    public UnoController(UnoGameRegistry registry, UnoSseService sseService) { this.registry = registry; this.sseService = sseService; }

    /** DTO for incoming commands. */
    public record UnoCommand(String type, String playerId, String color, String value) {}
    /** Simple command result DTO aligned with frontend expectations. */
    public record CommandResult(boolean applied, List<ErrorInfo> errors, Map<String,Object> view) {
        public static CommandResult ok(Map<String,Object> v) { return new CommandResult(true, List.of(), v); }
        public static CommandResult error(String msg, Map<String,Object> v) { return new CommandResult(false, List.of(new ErrorInfo(msg)), v); }
    }
    public record ErrorInfo(String message) {}

    @GetMapping("/{gameId}/view")
    public ResponseEntity<Map<String,Object>> getView(@PathVariable String gameId, @RequestParam String viewerId) {
        UnoRuntimePhase runtime = registry.get(gameId);
        if (runtime == null) return ResponseEntity.notFound().build();
        var backendView = runtime.buildView(viewerId);
        return ResponseEntity.ok(transformView(runtime, backendView));
    }

    @PostMapping("/{gameId}/commands")
    public ResponseEntity<CommandResult> command(@PathVariable String gameId, @RequestBody UnoCommand cmd) {
        UnoRuntimePhase runtime = registry.get(gameId);
        if (runtime == null) return ResponseEntity.ok(CommandResult.error("Game not found", null));
        // Ensure listener installed
        if (runtime.actionLogSnapshot().isEmpty()) { // crude guard for first-time
            runtime.setTurnListener(rt -> {
                // Broadcast generic perspective (no private hand exposure)
                var vGen = transformView(rt, rt.buildView(null));
                sseService.broadcastView(gameId, vGen);
            });
        }
        var result = runtime.applyPlayerCommand(new UnoRuntimePhase.PlayerCommand(cmd.type(), cmd.playerId(), cmd.color(), cmd.value()));
        var v = transformView(runtime, result.view());
        if (!result.applied()) {
            List<ErrorInfo> errs = result.errors().stream().map(e -> new ErrorInfo(e.code()+":"+e.message())).toList();
            // Broadcast even on error? Only if state mutated. For simplicity: broadcast always.
            sseService.broadcastView(gameId, v);
            return ResponseEntity.ok(new CommandResult(false, errs, v));
        }
        sseService.broadcastView(gameId, v);
        return ResponseEntity.ok(new CommandResult(true, List.of(), v));
    }

    /** SSE stream for live view updates. */
    @GetMapping("/{gameId}/stream")
    public SseEmitter stream(@PathVariable String gameId) {
        return sseService.subscribe(gameId);
    }

    /** Convert internal UnoView (string hand displays) into front-end expected structure. */
    private Map<String,Object> transformView(UnoRuntimePhase runtime, UnoView view) {
        Map<String,Object> out = new LinkedHashMap<>();
        UnoBoardView b = view.board();
        out.put("phase", "RUNTIME");
        out.put("turnCount", b.turnCount());
        String topDisp = b.topCard();
        if (topDisp != null) out.put("top", parseCard(topDisp));
        if (b.activeColor() != null) out.put("activeColor", b.activeColor());
        out.put("viewerId", view.perspectivePlayerId());
        // players
        List<Map<String,Object>> playerList = new ArrayList<>();
        for (int i=0;i<view.players().size();i++) {
            UnoPlayerView pv = view.players().get(i);
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("playerId", pv.playerId());
            p.put("bot", pv.bot());
            p.put("handSize", pv.handSize());
            p.put("isCurrent", i == b.currentPlayerIndex());
            p.put("isWinner", false); // placeholder
            if (pv.hand() != null) {
                List<Map<String,String>> hand = new ArrayList<>();
                for (String disp : pv.hand()) hand.add(parseCard(disp));
                p.put("hand", hand);
            }
            playerList.add(p);
        }
        out.put("players", playerList);
        // Stacking penalty exposure
        int pen = runtime.pendingDrawPenalty();
        out.put("pendingDraw", pen);
        if (pen > 0 && runtime.pendingPenaltyType() != null) {
            out.put("pendingDrawType", runtime.pendingPenaltyType().name());
        }
        boolean mustChoose = runtime.isAwaitingColorChoice();
        out.put("mustChooseColor", mustChoose);
        if (mustChoose) {
            out.put("colorChooser", runtime.awaitingColorChooser());
        }
        // --- Action events (Stage 1) ---
        var events = new ArrayList<Map<String,Object>>();
        for (var e : runtime.actionLogSnapshot()) {
            Map<String,Object> ev = new LinkedHashMap<>();
            ev.put("id", e.seq());
            ev.put("type", e.type());
            ev.put("actorId", e.actorId());
            ev.put("text", e.text());
            ev.put("ts", e.ts());
            events.add(ev);
        }
        out.put("events", events);
        out.put("lastEventSeq", runtime.lastEventSeq());
        return out;
    }

    private Map<String,String> parseCard(String display) {
        // Examples: "RED 5", "RED SKIP", "WILD", "WILD_DRAW_FOUR"
        Map<String,String> c = new LinkedHashMap<>();
        String[] parts = display.split(" ");
        if (parts.length == 1) {
            c.put("value", parts[0]);
            c.put("color", parts[0].startsWith("WILD") ? null : null); // wild has no fixed color
        } else if (parts.length == 2) {
            c.put("color", parts[0]);
            c.put("value", parts[1]);
        } else {
            c.put("value", display);
        }
        if (!c.containsKey("color")) c.put("color", parts.length==2?parts[0]:null);
        return c;
    }
}
