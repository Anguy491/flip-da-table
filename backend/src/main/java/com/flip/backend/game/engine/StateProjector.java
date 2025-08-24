package com.flip.backend.game.engine;

/**
 * Optional read-model projector: transform full state S to client-scoped view V.
 * Useful for hiding private info (e.g., other players' hands).
 */
public interface StateProjector<S, V> {
    V toView(S state, String viewerId);
}