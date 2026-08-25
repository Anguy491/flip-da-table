package com.flip.backend.lasvegas.bot;

import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;

/** Immutable optimistic guard for one delayed bot action. */
public record LasVegasBotTicket(
        String gameId,
        long expectedVersion,
        LasVegasRuntimePhase.State expectedPhase,
        String botId
) {}
