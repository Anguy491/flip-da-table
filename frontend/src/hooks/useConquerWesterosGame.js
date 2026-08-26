import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getLatestGame } from '../api/sessions';
import { getConquerWesterosView, sendConquerWesterosCommand } from '../api/conquerWesteros';

export default function useConquerWesterosGame({
  sessionId,
  initialGameId,
  initialPlayerId,
  initialView,
  token,
}) {
  const [gameId, setGameId] = useState(initialGameId || null);
  const [playerId, setPlayerId] = useState(initialPlayerId || null);
  const [view, setView] = useState(initialView || null);
  const [loading, setLoading] = useState(!initialView);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [connectionState, setConnectionState] = useState('connecting');
  const [publicEvents, setPublicEvents] = useState([]);
  const busyRef = useRef(false);
  const eventTimerRef = useRef(null);

  const showPublicEvents = useCallback((events) => {
    if (!Array.isArray(events) || !events.length) return;
    setPublicEvents(events);
    window.clearTimeout(eventTimerRef.current);
    eventTimerRef.current = window.setTimeout(() => setPublicEvents([]), 2600);
  }, []);

  const hydrateLatest = useCallback(async () => {
    if (!sessionId || !token) return null;
    const latest = await getLatestGame(sessionId, token);
    if (latest.gameType !== 'CONQUERWESTEROS') throw new Error('The latest game is not Conquer Westeros.');
    setGameId(latest.gameId);
    setPlayerId(latest.myPlayerId);
    setView(latest.view);
    return latest;
  }, [sessionId, token]);

  const reload = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      if (gameId) setView(await getConquerWesterosView(gameId, token));
      else await hydrateLatest();
      setError('');
    } catch (requestError) {
      setError(requestError.message || 'Failed to load the war table.');
    } finally {
      setLoading(false);
    }
  }, [gameId, hydrateLatest, token]);

  useEffect(() => { void reload(); }, [reload]);
  useEffect(() => () => window.clearTimeout(eventTimerRef.current), []);

  useEffect(() => {
    if (!gameId || !playerId || !token) {
      setConnectionState('offline');
      return undefined;
    }
    let client;
    let active = true;
    setConnectionState('connecting');
    import('@stomp/stompjs').then(({ Client }) => {
      if (!active) return;
      client = new Client({
        brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 3000,
        onConnect: () => {
          if (!active) return;
          setConnectionState('connected');
          client.subscribe(`/topic/conquer-westeros/${gameId}/${playerId}`, (message) => {
            try {
              setView(JSON.parse(message.body));
              setError('');
            } catch {
              setError('A private table update could not be read.');
            }
          });
          client.subscribe(`/topic/conquer-westeros/${gameId}/events`, (message) => {
            try { showPublicEvents(JSON.parse(message.body)); }
            catch { setError('A battle event could not be read.'); }
          });
        },
        onWebSocketClose: () => { if (active) setConnectionState('reconnecting'); },
        onStompError: () => { if (active) setConnectionState('offline'); },
      });
      client.activate();
    }).catch(() => setConnectionState('offline'));
    return () => {
      active = false;
      if (client) void client.deactivate();
    };
  }, [gameId, playerId, showPublicEvents, token]);

  useEffect(() => {
    if (!gameId || view?.phase === 'FINISHED' || !['reconnecting', 'offline'].includes(connectionState)) return undefined;
    const timer = window.setInterval(() => { void reload(); }, 3000);
    return () => window.clearInterval(timer);
  }, [connectionState, gameId, reload, view?.phase]);

  const send = useCallback(async (command) => {
    if (!gameId || !view || busyRef.current || view.phase === 'FINISHED') return;
    busyRef.current = true;
    setSending(true);
    setError('');
    try {
      const response = await sendConquerWesterosCommand(gameId, {
        expectedVersion: view.stateVersion,
        ...command,
      }, token);
      setView(response.view);
      showPublicEvents(response.publicEvents);
    } catch (requestError) {
      if (requestError.status === 409) {
        setError('The table advanced elsewhere. Your view has been resynced.');
        await reload();
      } else {
        setError(requestError.message || 'The war table rejected this action.');
      }
    } finally {
      busyRef.current = false;
      setSending(false);
    }
  }, [gameId, reload, showPublicEvents, token, view]);

  const me = useMemo(() => view?.players?.find((player) => player.playerId === playerId), [playerId, view?.players]);
  const currentPlayer = useMemo(() => view?.players?.find((player) => player.playerId === view?.currentPlayerId), [view?.currentPlayerId, view?.players]);

  return {
    gameId,
    playerId,
    view,
    me,
    currentPlayer,
    loading,
    sending,
    error,
    connectionState,
    publicEvents,
    isMyTurn: Boolean(playerId && view?.currentPlayerId === playerId),
    actions: {
      roll: () => void send({ type: 'ROLL_DICE' }),
      completeLine: (targetId, lineId, dieIds) => void send({ type: 'COMPLETE_LINE', targetId, lineId, dieIds }),
      loseDie: (dieId) => void send({ type: 'LOSE_DIE', dieId }),
      reload: () => void reload(),
    },
  };
}
