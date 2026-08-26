package com.flip.backend.conquerwesteros.engine.event;

import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import com.flip.backend.conquerwesteros.entities.ConquerWesterosPlayer;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.game.engine.event.GameEvent;

import java.util.List;

/** Rule events used by the aggregate's synchronous event queue. */
public final class ConquerWesterosEvents {
    private ConquerWesterosEvents() {}

    private abstract static class RuleEvent extends GameEvent {
        protected final ConquerWesterosRuntimePhase runtime;
        protected final ConquerWesterosPlayer player;

        RuleEvent(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player) {
            super(player, System.currentTimeMillis());
            this.runtime = runtime;
            this.player = player;
        }
    }

    public static final class Roll extends RuleEvent {
        public Roll(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player) { super(runtime, player); }
        @Override public boolean isValid() { return runtime.canRoll(player); }
        @Override public void execute() { runtime.rollDice(player); }
    }

    public static final class CompleteLine extends RuleEvent {
        private final String targetId;
        private final String lineId;
        private final List<Integer> dieIds;
        private final EventQueue queue;

        public CompleteLine(
                ConquerWesterosRuntimePhase runtime,
                ConquerWesterosPlayer player,
                String targetId,
                String lineId,
                List<Integer> dieIds,
                EventQueue queue
        ) {
            super(runtime, player);
            this.targetId = targetId;
            this.lineId = lineId;
            this.dieIds = dieIds == null ? List.of() : List.copyOf(dieIds);
            this.queue = queue;
        }

        @Override public boolean isValid() { return runtime.canCompleteLine(player, targetId, lineId, dieIds); }
        @Override public void execute() { runtime.completeLine(player, targetId, lineId, dieIds, queue); }
    }

    public static final class LoseDie extends RuleEvent {
        private final int dieId;
        private final EventQueue queue;

        public LoseDie(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player, int dieId, EventQueue queue) {
            super(runtime, player);
            this.dieId = dieId;
            this.queue = queue;
        }

        @Override public boolean isValid() { return runtime.canLoseDie(player, dieId); }
        @Override public void execute() { runtime.loseDie(player, dieId, queue); }
    }

    public static final class SiegeFailed extends RuleEvent {
        private final EventQueue queue;
        public SiegeFailed(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player, EventQueue queue) {
            super(runtime, player);
            this.queue = queue;
        }
        @Override public boolean isValid() { return runtime.canFailSiege(player); }
        @Override public void execute() { runtime.failSiege(player, queue); }
    }

    public static final class Capture extends RuleEvent {
        private final EventQueue queue;
        public Capture(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player, EventQueue queue) {
            super(runtime, player);
            this.queue = queue;
        }
        @Override public boolean isValid() { return runtime.canCapture(player); }
        @Override public void execute() { runtime.capture(player, queue); }
    }

    public static final class TransferIronThrone extends RuleEvent {
        private final String reason;
        public TransferIronThrone(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player, String reason) {
            super(runtime, player);
            this.reason = reason;
        }
        @Override public boolean isValid() { return runtime.canTransferIronThrone(player); }
        @Override public void execute() { runtime.transferIronThrone(player, reason); }
    }

    public static final class CompleteClan extends RuleEvent {
        private final String clan;
        public CompleteClan(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player, String clan) {
            super(runtime, player);
            this.clan = clan;
        }
        @Override public boolean isValid() { return runtime.canCompleteClan(player, clan); }
        @Override public void execute() { runtime.completeClan(player, clan); }
    }

    public static final class AdvanceTurn extends RuleEvent {
        public AdvanceTurn(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player) { super(runtime, player); }
        @Override public boolean isValid() { return runtime.canAdvanceTurn(); }
        @Override public void execute() { runtime.advanceTurn(); }
    }

    public static final class EndGame extends RuleEvent {
        public EndGame(ConquerWesterosRuntimePhase runtime, ConquerWesterosPlayer player) { super(runtime, player); }
        @Override public boolean isValid() { return runtime.canEndGame(); }
        @Override public void execute() { runtime.endGame(); }
    }
}
