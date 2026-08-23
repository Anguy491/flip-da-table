package com.flip.backend.api;

import com.flip.backend.dvc.engine.DVCGameRegistry;
import com.flip.backend.dvc.engine.DVCStartRegistry;
import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import com.flip.backend.service.game.DVCGameService;
import com.flip.backend.security.GameAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DVCControllerSettleTest {
    @Test
    void humanConfirmationTransitionsPastStartWhenTheOtherSeatIsABot() {
        var runtimeRegistry = new DVCGameRegistry();
        var startRegistry = new DVCStartRegistry();
        var ws = mock(DvcWsService.class);
        var access = mock(GameAccessService.class);
        var authentication = mock(Authentication.class);
        var controller = new DVCController(runtimeRegistry, startRegistry, mock(DVCGameService.class), ws, access);
        var start = new DVCStartPhase(List.of("P1_HUMAN", "BOT1"));
        start.enter();
        startRegistry.put("game-1", start);
        var human = start.players().stream().filter(player -> !player.isBot()).findFirst().orElseThrow();
        when(access.requireClaimedPlayer(authentication, "game-1", human.getId())).thenReturn(human.getId());
        String hand = human.hand().snapshot().stream().map(card -> card.cardId()).collect(Collectors.joining());

        boolean accepted = controller.settle(
            "game-1",
            new DVCController.SettleRequest(human.getId(), true, hand),
            authentication
        );

        assertTrue(accepted);
        assertFalse(startRegistry.exists("game-1"));
        var runtime = runtimeRegistry.get("game-1");
        assertNotNull(runtime);
        verify(ws).broadcastRuntime(eq("game-1"), same(runtime));
    }
}
