package com.flip.backend.api;

import com.flip.backend.dvc.engine.phase.DVCRuntimePhase;
import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import com.flip.backend.dvc.engine.view.DVCView;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class DvcWsService {
    private final SimpMessagingTemplate messaging;
    public DvcWsService(SimpMessagingTemplate messaging) { this.messaging = messaging; }

    public void broadcastStart(String gameId, DVCStartPhase sp) {
        if (sp == null) return;
        for (var p : sp.players()) {
            DVCView v = sp.buildView(p.getId());
            messaging.convertAndSend("/topic/dvc/" + gameId + "/" + p.getId(), v);
        }
    }

    public void broadcastRuntime(String gameId, DVCRuntimePhase rt) {
        if (rt == null) return;
        for (var p : rt.players()) {
            DVCView v = rt.buildView(p.getId());
            messaging.convertAndSend("/topic/dvc/" + gameId + "/" + p.getId(), v);
        }
    }
}
