import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getUnoView, unoCommand } from '../api/uno';

// card util helper
function matchesTop(card, top) {
  if (!top) return true;
  return card.value === top.value || (card.color && top.color && card.color === top.color) || card.value === 'WILD' || card.value === 'WILD_DRAW_FOUR';
}

export default function useUnoGame({ gameId, playerId, token, autoPoll = false, pollMs = 5000 }) {
  const [view, setView] = useState(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [lastResult, setLastResult] = useState(null);
  const timerRef = useRef(null);

  const load = useCallback(async () => {
  if (!gameId || !playerId) { setLoading(false); return; }
    try {
      setLoading(true); setError('');
      const v = await getUnoView(gameId, playerId, token);
      setView(v);
    } catch (e) {
      setError(e.message || 'Failed to load view');
    } finally { setLoading(false); }
  }, [gameId, playerId, token]);

  useEffect(() => { load(); }, [load]);

  // polling optional (for future multi-human games)
  useEffect(() => {
    if (!autoPoll || !view) return;
    if (view.phase === 'FINISHED') return; // stop after finish
    timerRef.current && clearInterval(timerRef.current);
    timerRef.current = setInterval(() => { load(); }, pollMs);
    return () => timerRef.current && clearInterval(timerRef.current);
  }, [autoPoll, view, load, pollMs]);

  const me = useMemo(() => view?.players?.find(p => p.playerId === playerId), [view, playerId]);
  const current = useMemo(() => view?.players?.find(p => p.isCurrent), [view]);
  const myTurn = !!current && current.playerId === playerId;
  const hand = me?.hand || [];
  const pendingDraw = view?.pendingDraw || 0;
  const mustChooseColor = !!view?.mustChooseColor;

  // playable cards filtering (front-end rule gating)
  const playableCards = useMemo(() => {
    if (!myTurn || mustChooseColor) return [];
    const top = view?.top;
    return hand.filter(c => {
      if (pendingDraw > 0) {
        return c.value === 'DRAW_TWO' || c.value === 'WILD_DRAW_FOUR';
      }
      return matchesTop(c, top);
    });
  }, [myTurn, mustChooseColor, hand, view, pendingDraw]);

  const canDraw = myTurn && !mustChooseColor;
  const canDeclareUno = myTurn && hand.length === 2 && !mustChooseColor;
  const isFinished = view?.phase === 'FINISHED';

  const applyResult = (resp) => {
    setLastResult(resp);
    if (resp?.view) setView(resp.view);
    if (!resp.applied && resp.errors?.length) {
      setError(resp.errors.map(e => e.message).join('; '));
    } else {
      setError('');
    }
  };

  const send = useCallback(async (command) => {
    if (sending || !gameId) return;
    setSending(true);
    try {
      const resp = await unoCommand(gameId, command, token);
      applyResult(resp);
    } catch (e) {
      setError(e.message || 'Command failed');
    } finally { setSending(false); }
  }, [gameId, token, sending]);

  const playCard = useCallback((card) => {
    if (!myTurn || mustChooseColor) return;
    // ensure card playable locally
    if (!playableCards.some(c => c === card || (c.color === card.color && c.value === card.value))) return;
    send({ type: 'PLAY_CARD', playerId, color: card.color, value: card.value });
  }, [myTurn, mustChooseColor, playableCards, send, playerId]);

  const drawCard = useCallback(() => {
    if (!canDraw) return;
    send({ type: 'DRAW_CARD', playerId });
  }, [canDraw, send, playerId]);

  const chooseColor = useCallback((color) => {
    if (!mustChooseColor || !myTurn) return;
    send({ type: 'CHOOSE_COLOR', playerId, color });
  }, [mustChooseColor, myTurn, send, playerId]);

  const declareUno = useCallback(() => {
    if (!canDeclareUno) return;
    send({ type: 'DECLARE_UNO', playerId });
  }, [canDeclareUno, send, playerId]);

  return {
    view, loading, sending, error, lastResult,
    myTurn, hand, playableCards, canDraw, canDeclareUno, mustChooseColor, pendingDraw, isFinished,
    actions: { playCard, drawCard, chooseColor, declareUno, reload: load },
  };
}
