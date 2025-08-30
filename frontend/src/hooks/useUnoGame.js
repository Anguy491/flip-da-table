import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getUnoView, unoCommand } from '../api/uno';

// card util helper (includes activeColor after wild plays)
function matchesTop(card, top, activeColor) {
  if (!top) return true;
  const isTopWild = top.value === 'WILD' || top.value === 'WILD_DRAW_FOUR';
  if (isTopWild && activeColor) {
    // When top is a wild and a color has been chosen, only that color (plus any wilds) is playable
    return card.value === 'WILD' || card.value === 'WILD_DRAW_FOUR' || card.color === activeColor;
  }
  // Normal matching: same value OR same color OR wilds
  return card.value === top.value || (card.color && top.color && card.color === top.color) || card.value === 'WILD' || card.value === 'WILD_DRAW_FOUR';
}

export default function useUnoGame({ gameId, playerId, token, autoPoll = false, pollMs = 5000 }) {
  const [view, setView] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [lastResult, setLastResult] = useState(null);
  const timerRef = useRef(null);
  const sseRef = useRef(null);
  const sseFailures = useRef(0);

  const load = useCallback(async () => {
  if (!gameId || !playerId) { setLoading(false); return; }
    try {
      setLoading(true); setError('');
      const v = await getUnoView(gameId, playerId, token);
      setView(v);
  if (Array.isArray(v?.events)) setEvents(v.events);
    } catch (e) {
      setError(e.message || 'Failed to load view');
    } finally { setLoading(false); }
  }, [gameId, playerId, token]);

  useEffect(() => { load(); }, [load]);

  // SSE setup (Stage 2)
  useEffect(() => {
    if (!gameId || !playerId || !token) return;
    // Avoid duplicate
    if (sseRef.current) return;
    try {
      const ev = new EventSource(`/api/games/uno/${gameId}/stream`);
      sseRef.current = ev;
      ev.onmessage = (m) => {
        // Default unnamed events (INIT) ignored; named handled in addEventListener below
      };
      ev.addEventListener('VIEW', (e) => {
        try {
          const payload = JSON.parse(e.data);
          // Because SSE broadcast uses null perspective, we only take public fields (no private hand) unless a command response overrides later.
          setView(v => ({ ...(v||{}), ...payload }));
          if (Array.isArray(payload?.events)) setEvents(payload.events);
        } catch {/* ignore parse errors */}
      });
      ev.onerror = () => {
        sseFailures.current += 1;
        ev.close();
        sseRef.current = null;
      };
    } catch (e) {
      sseFailures.current += 1;
    }
    return () => { if (sseRef.current) { sseRef.current.close(); sseRef.current = null; } };
  }, [gameId, playerId, token]);

  // polling optional (for future multi-human games)
  useEffect(() => {
    const needPollingFallback = autoPoll || (!sseRef.current && sseFailures.current > 0);
    if (!needPollingFallback || !view) return;
    if (view.phase === 'FINISHED') return;
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
    const activeColor = view?.activeColor;
    return hand.filter(c => {
      if (pendingDraw > 0) {
        return c.value === 'DRAW_TWO' || c.value === 'WILD_DRAW_FOUR';
      }
      return matchesTop(c, top, activeColor);
    });
  }, [myTurn, mustChooseColor, hand, view, pendingDraw]);

  const canDraw = myTurn && !mustChooseColor;
  const canDeclareUno = myTurn && hand.length === 2 && !mustChooseColor;
  const isFinished = view?.phase === 'FINISHED';

  const applyResult = (resp) => {
    setLastResult(resp);
    if (resp?.view) setView(resp.view);
  if (Array.isArray(resp?.view?.events)) setEvents(resp.view.events);
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
  events,
    myTurn, hand, playableCards, canDraw, canDeclareUno, mustChooseColor, pendingDraw, isFinished,
    actions: { playCard, drawCard, chooseColor, declareUno, reload: load },
  };
}
