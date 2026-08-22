import { useState, useCallback, useMemo, useEffect } from 'react';
import { parseCard } from '../components/dvc/parseCard';

export function useDVCGame({ view, myPlayerId }) {
  const board = view?.board;
  const awaiting = board?.awaiting;
  const playerViews = useMemo(() => view?.players || [], [view?.players]);
  const meView = useMemo(() => playerViews.find((player) => player.playerId === myPlayerId), [myPlayerId, playerViews]);
  const myPending = meView?.pending || null;
  const rawCards = useMemo(() => meView?.cards || [], [meView?.cards]);
  const parsedHand = useMemo(() => rawCards.map(parseCard), [rawCards]);
  const handKey = useMemo(() => JSON.stringify(rawCards), [rawCards]);
  const [localOrder, setLocalOrder] = useState(null);

  useEffect(() => { setLocalOrder(null); }, [handKey]);

  const effectiveHand = useMemo(
    () => localOrder ? localOrder.map((index) => parsedHand[index]) : parsedHand,
    [localOrder, parsedHand],
  );

  const isMyTurn = useMemo(() => {
    if (!board) return false;
    if (board.awaiting === 'SETTLE_POSITION') return true;
    return playerViews[board.currentPlayerIndex]?.playerId === myPlayerId;
  }, [board, myPlayerId, playerViews]);

  const canDragInitial = awaiting === 'SETTLE_POSITION' && !myPending;
  const canDragPending = awaiting === 'SETTLE_POSITION' && Boolean(myPending);

  const reorderHand = useCallback((from, to) => {
    if (!canDragInitial && !canDragPending) return;
    setLocalOrder((current) => {
      const indexes = current ? [...current] : parsedHand.map((_, index) => index);
      const [moved] = indexes.splice(from, 1);
      indexes.splice(to, 0, moved);
      return indexes;
    });
  }, [canDragInitial, canDragPending, parsedHand]);

  return {
    awaiting,
    board,
    parsedHand: effectiveHand,
    isMyTurn,
    canDragInitial,
    canDragPending,
    canSelectOpponentCard: awaiting === 'GUESS_SELECTION' && isMyTurn,
    showGuessPrompt: awaiting === 'GUESS_SELECTION' && isMyTurn,
    showDrawColorModal: awaiting === 'DRAW_COLOR' && isMyTurn,
    reorderHand,
    resetLocalOrder: () => setLocalOrder(null),
    localOrder,
  };
}
