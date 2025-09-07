import { useState, useCallback, useMemo, useEffect } from 'react';
import { parseCard } from '../components/dvc/parseCard';

/**
 * useDVCGame: encapsulate client-side guard logic & derived UI permissions.
 * Responsibilities:
 *  - Track local drag-reordered hand (initial settle & pending settle)
 *  - Expose booleans for interactive states based on awaiting + turn ownership
 *  - Filter illegal actions before hitting API
 */
export function useDVCGame({ view, myPlayerId }) {
  const board = view?.board;
  const awaiting = board?.awaiting;
  const playerViews = view?.players || [];
  const meView = playerViews.find(p => p.playerId === myPlayerId);
  const myPending = meView?.pending || null;
  const rawCards = meView?.cards || [];
  const parsedHand = useMemo(()=> rawCards.map(parseCard), [rawCards]);
  // When server-provided hand strings change, drop any local reordering to adopt server order
  const handKey = useMemo(() => JSON.stringify(rawCards), [rawCards]);

  // Local ordering (indices) while arranging before initial settle (does not persist to server yet)
  const [localOrder, setLocalOrder] = useState(null); // null means not modified
  useEffect(() => { setLocalOrder(null); }, [handKey]);

  const effectiveHand = localOrder ? localOrder.map(i => parsedHand[i]) : parsedHand;

  const isMyTurn = useMemo(()=> {
    if (!board) return false;
    // During settle: start-phase (no pending) everyone can act; runtime settle (has pending) only self can act
    if (board.awaiting === 'SETTLE_POSITION') {
      return myPending ? true : true; // start-phase also true
    }
    const currentId = playerViews[board.currentPlayerIndex]?.playerId;
    return currentId === myPlayerId;
  }, [board, playerViews, myPlayerId, myPending]);

  // Interaction permissions
  const canDragInitial = awaiting === 'SETTLE_POSITION' && !myPending; // pre-game settle (no pending)
  const canDragPending = awaiting === 'SETTLE_POSITION' && !!myPending; // runtime settle (has pending)
  const canSelectOpponentCard = awaiting === 'GUESS_SELECTION' && isMyTurn;
  const showGuessPrompt = awaiting === 'GUESS_SELECTION' && isMyTurn;
  const showDrawColorModal = awaiting === 'DRAW_COLOR' && isMyTurn;

  const reorderHand = useCallback((from, to) => {
    if (!canDragInitial && !canDragPending) return;
    setLocalOrder(prev => {
      const baseIdx = prev ? [...prev] : parsedHand.map((_,i)=>i);
      const [m] = baseIdx.splice(from,1);
      baseIdx.splice(to,0,m);
      return baseIdx;
    });
  }, [canDragInitial, canDragPending, parsedHand]);

  const resetLocalOrder = () => setLocalOrder(null);

  return {
    awaiting,
    board,
    parsedHand: effectiveHand,
    isMyTurn,
    canDragInitial,
    canDragPending,
    canSelectOpponentCard,
    showGuessPrompt,
    showDrawColorModal,
    reorderHand,
    resetLocalOrder,
  localOrder
  };
}
