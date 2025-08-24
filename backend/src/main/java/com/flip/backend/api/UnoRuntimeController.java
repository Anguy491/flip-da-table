package com.flip.backend.api;

import com.flip.backend.api.dto.game.CommandResponseDto;
import com.flip.backend.api.dto.game.UnoCommandDto;
import com.flip.backend.game.service.GameRuntimeService;
import com.flip.backend.game.uno.*;
import com.flip.backend.game.uno.command.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UNO runtime endpoints (isolated from generic facade to allow future multi-game expansion). */
@RestController
@RequestMapping("/api/games/uno")
public class UnoRuntimeController {
    private final GameRuntimeService runtimeService;

    public UnoRuntimeController(GameRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    /** Get projected view. viewerId 暂用 playerId. */
    @GetMapping("/{gameId}/view")
    public ResponseEntity<UnoView> getView(@PathVariable String gameId, @RequestParam String viewerId) {
        return ResponseEntity.ok(runtimeService.getUnoView(gameId, viewerId));
    }

    /** Submit UNO command. */
    @PostMapping("/{gameId}/commands")
    public ResponseEntity<CommandResponseDto> command(@PathVariable String gameId, @Valid @RequestBody UnoCommandDto dto) {
        var result = runtimeService.handleUnoCommand(gameId, toCommand(dto));
        var errors = result.validation().isOk() ? List.<CommandResponseDto.ValidationError>of() :
                result.validation().errors().stream()
                        .map(it -> new CommandResponseDto.ValidationError(it.code().name(), it.message()))
                        .toList();
        // events simple map -> record toString json fallback
    List<?> events = result.events();
        var view = runtimeService.getUnoView(gameId, dto.playerId());
    return ResponseEntity.ok(new CommandResponseDto(result.applied(), errors, events, view));
    }

    private UnoCommand toCommand(UnoCommandDto dto) {
        return switch (dto.type().toUpperCase()) {
            case "PLAY_CARD" -> new PlayCard(dto.playerId(), dto.color(), dto.value());
            case "DRAW_CARD" -> new DrawCard(dto.playerId());
            case "PASS_TURN" -> new PassTurn(dto.playerId());
            case "CHOOSE_COLOR" -> new ChooseColor(dto.playerId(), dto.color());
            case "DECLARE_UNO" -> new DeclareUno(dto.playerId());
            default -> throw new IllegalArgumentException("Unknown command type: " + dto.type());
        };
    }
}
