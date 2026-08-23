import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getUnoView, unoCommand } from '../api/uno';

function matchesTop(card, top, activeColor) {
  if (!top) return true;
  const wild = card.value === 'WILD' || card.value === 'WILD_DRAW_FOUR';
  const topIsWild = top.value === 'WILD' || top.value === 'WILD_DRAW_FOUR';
  if (topIsWild && activeColor) return wild || card.color === activeColor;
  return wild || card.value === top.value || (card.color && top.color && card.color === top.color);
}

export function canPlayUnoCard(card, { top, activeColor, pendingDraw = 0, pendingDrawType } = {}) {
  if (pendingDraw > 0) {
    const requiredPenaltyType = pendingDrawType
      || (top?.value === 'DRAW_TWO' || top?.value === 'WILD_DRAW_FOUR' ? top.value : null);
    return Boolean(requiredPenaltyType) && card.value === requiredPenaltyType;
  }
  return matchesTop(card, top, activeColor);
}

export function parseSseBlock(block) {
  let event = 'message';
  const data = [];
  for (const line of block.split(/\r?\n/)) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
  }
  return { event, data: data.join('\n') };
}

export default function useUnoGame({ gameId, playerId, token, autoPoll = false, pollMs = 5000 }) {
  const [view, setView] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [lastResult, setLastResult] = useState(null);
  const [connectionState, setConnectionState] = useState('connecting');
  const pollTimerRef = useRef(null);

  const load = useCallback(async () => {
    if (!gameId || !playerId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const nextView = await getUnoView(gameId, playerId, token);
      setView(nextView);
      if (Array.isArray(nextView?.events)) setEvents(nextView.events);
      setError('');
    } catch (requestError) {
      setError(requestError.message || 'Failed to load the table.');
    } finally {
      setLoading(false);
    }
  }, [gameId, playerId, token]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!gameId || !playerId || !token) {
      setConnectionState('offline');
      return undefined;
    }

    let abortController;
    let reconnectTimer;
    let stopped = false;

    const applyLiveView = (payload) => {
      setView((currentView) => {
        if (!currentView) return payload;
        const currentPlayers = Array.isArray(currentView.players) ? currentView.players : [];
        const incomingPlayers = Array.isArray(payload.players) ? payload.players : [];
        const mergedPlayers = incomingPlayers.map((incoming) => {
          const current = currentPlayers.find((player) => player.playerId === incoming.playerId);
          return incoming.playerId === playerId && !incoming.hand && current?.hand
            ? { ...incoming, hand: current.hand }
            : incoming;
        });
        return { ...currentView, ...payload, players: mergedPlayers };
      });
      if (Array.isArray(payload?.events)) setEvents(payload.events);
      setConnectionState('connected');
    };

    const connect = async () => {
      if (stopped) return;
      setConnectionState((current) => current === 'connected' ? 'reconnecting' : 'connecting');
      abortController = new AbortController();
      try {
        const response = await fetch(`/api/games/uno/${gameId}/stream`, {
          headers: {
            Accept: 'text/event-stream',
            Authorization: `Bearer ${token}`,
          },
          signal: abortController.signal,
        });
        if (!response.ok || !response.body) throw new Error(`Stream failed: ${response.status}`);
        setConnectionState('connected');
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        while (!stopped) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const blocks = buffer.split(/\r?\n\r?\n/);
          buffer = blocks.pop() || '';
          for (const block of blocks) {
            const parsed = parseSseBlock(block);
            if (parsed.event !== 'VIEW' || !parsed.data) continue;
            try {
              applyLiveView(JSON.parse(parsed.data));
            } catch {
              setError('A live table update could not be read.');
            }
          }
        }
      } catch (streamError) {
        if (stopped || streamError?.name === 'AbortError') return;
      }
      if (!stopped) {
        setConnectionState('reconnecting');
        reconnectTimer = window.setTimeout(() => { void connect(); }, 3000);
      }
    };

    void connect();
    return () => {
      stopped = true;
      window.clearTimeout(reconnectTimer);
      abortController?.abort();
    };
  }, [gameId, playerId, token]);

  useEffect(() => {
    const usePolling = autoPoll || connectionState === 'reconnecting' || connectionState === 'offline';
    if (!usePolling || view?.phase === 'FINISHED') return undefined;
    window.clearInterval(pollTimerRef.current);
    pollTimerRef.current = window.setInterval(() => { void load(); }, pollMs);
    return () => window.clearInterval(pollTimerRef.current);
  }, [autoPoll, connectionState, load, pollMs, view]);

  const me = useMemo(() => view?.players?.find((player) => player.playerId === playerId), [playerId, view]);
  const current = useMemo(() => view?.players?.find((player) => player.isCurrent), [view]);
  const myTurn = Boolean(current && current.playerId === playerId);
  const hand = useMemo(() => me?.hand || [], [me?.hand]);
  const pendingDraw = view?.pendingDraw || 0;
  const pendingDrawType = view?.pendingDrawType;
  const mustChooseColor = Boolean(view?.mustChooseColor);

  const playableCards = useMemo(() => {
    if (!myTurn || mustChooseColor) return [];
    return hand.filter((card) => canPlayUnoCard(card, {
      top: view?.top,
      activeColor: view?.activeColor,
      pendingDraw,
      pendingDrawType,
    }));
  }, [hand, mustChooseColor, myTurn, pendingDraw, pendingDrawType, view?.activeColor, view?.top]);

  const canDraw = myTurn && !mustChooseColor;
  const canDeclareUno = myTurn && hand.length === 2 && !mustChooseColor;
  const isFinished = view?.phase === 'FINISHED';

  const applyResult = useCallback((response) => {
    setLastResult(response);
    if (response?.view) setView(response.view);
    if (Array.isArray(response?.view?.events)) setEvents(response.view.events);
    if (!response?.applied && response?.errors?.length) {
      setError(response.errors.map((item) => item.message).join('; '));
    } else {
      setError('');
    }
  }, []);

  const send = useCallback(async (command) => {
    if (sending || !gameId) return;
    setSending(true);
    try {
      applyResult(await unoCommand(gameId, command, token));
    } catch (requestError) {
      setError(requestError.message || 'Command failed.');
    } finally {
      setSending(false);
    }
  }, [applyResult, gameId, sending, token]);

  const playCard = useCallback((card) => {
    if (!myTurn || mustChooseColor) return;
    if (!playableCards.some((candidate) => candidate === card || (candidate.color === card.color && candidate.value === card.value))) return;
    void send({ type: 'PLAY_CARD', playerId, color: card.color, value: card.value });
  }, [mustChooseColor, myTurn, playableCards, playerId, send]);

  const drawCard = useCallback(() => {
    if (canDraw) void send({ type: 'DRAW_CARD', playerId });
  }, [canDraw, playerId, send]);

  const chooseColor = useCallback((color) => {
    if (mustChooseColor && myTurn) void send({ type: 'CHOOSE_COLOR', playerId, color });
  }, [mustChooseColor, myTurn, playerId, send]);

  const declareUno = useCallback(() => {
    if (canDeclareUno) void send({ type: 'DECLARE_UNO', playerId });
  }, [canDeclareUno, playerId, send]);

  return {
    view,
    loading,
    sending,
    error,
    lastResult,
    events,
    connectionState,
    myTurn,
    hand,
    playableCards,
    canDraw,
    canDeclareUno,
    mustChooseColor,
    pendingDraw,
    isFinished,
    actions: { playCard, drawCard, chooseColor, declareUno, reload: load },
  };
}
