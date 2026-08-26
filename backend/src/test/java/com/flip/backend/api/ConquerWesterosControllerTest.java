package com.flip.backend.api;

import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.service.game.ConquerWesterosGameService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConquerWesterosControllerTest {
    @Test
    void derivesThePlayerFromAuthenticationForViewsAndCommands() {
        var service = mock(ConquerWesterosGameService.class);
        var access = mock(GameAccessService.class);
        var authentication = mock(Authentication.class);
        var view = mock(GameView.class);
        when(access.requirePlayer(authentication, "game-1")).thenReturn("P1");
        when(service.viewFor("game-1", "P1")).thenReturn(view);
        var outcome = new ConquerWesterosGameService.CommandOutcome(view, List.of());
        when(service.command(eq("game-1"), eq("P1"), org.mockito.ArgumentMatchers.any())).thenReturn(outcome);
        var controller = new ConquerWesterosController(service, access);

        assertEquals(view, controller.view("game-1", authentication).getBody());
        var response = controller.command("game-1", new ConquerWesterosController.CommandRequest(
                7, "COMPLETE_LINE", "T05", "L1", List.of(0, 1), null), authentication).getBody();

        assertEquals(view, response.view());
        var commandCaptor = org.mockito.ArgumentCaptor.forClass(ConquerWesterosRuntimePhase.Command.class);
        verify(service).command(eq("game-1"), eq("P1"), commandCaptor.capture());
        assertEquals(7, commandCaptor.getValue().expectedVersion());
        assertEquals("T05", commandCaptor.getValue().targetId());
        assertEquals(List.of(0, 1), commandCaptor.getValue().dieIds());
    }
}
