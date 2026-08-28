package com.flip.backend.conquerwesteros.bot;

import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;

/** Immutable optimistic guard for one delayed Bot action. */
public record ConquerWesterosBotTicket(
        String gameId,
        long expectedVersion,
        ConquerWesterosRuntimePhase.State expectedPhase,
        String botId
) {}
