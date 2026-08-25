import { useParams, useNavigate } from 'react-router-dom';
import { useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { AuthContext } from '../context/auth-context';
import PageContainer from '../components/PageContainer';
import ErrorPopup from '../components/ErrorPopup';
import {
  ArcadeBadge,
  ArcadeButton,
  ArcadePanel,
  ArcadeSelect,
  ConnectionBadge,
  PlayerSeat,
} from '../components/arcade/ArcadeUI';
import { startFirstGame, getSession } from '../api/sessions';

function userIdFromToken(token) {
  if (!token) return null;
  try {
    const encoded = token.split('.')[1];
    const json = atob(encoded.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json)?.uid ?? null;
  } catch {
    return null;
  }
}

export default function Lobby({ preview = null }) {
  const { sessionid: routeSessionId } = useParams();
  const sessionid = preview?.sessionId || routeSessionId;
  const navigate = useNavigate();
  const { token } = useContext(AuthContext);
  const tokenUserId = useMemo(() => userIdFromToken(token), [token]);
  const myUserId = preview?.myUserId || tokenUserId;
  const [players, setPlayers] = useState(preview?.players || []);
  const [sessionInfo, setSessionInfo] = useState(preview?.sessionInfo || null);
  const [loadingSession, setLoadingSession] = useState(!preview);
  const [rounds, setRounds] = useState(preview?.rounds || 1);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [connectionState, setConnectionState] = useState(preview?.connectionState || 'connecting');

  const gameType = sessionInfo?.gameType?.toUpperCase();
  const capabilities = sessionInfo?.capabilities || {
    minPlayers: gameType === 'DAVINCI' || gameType === 'UNO' ? 2 : gameType === 'LASVEGAS' ? 3 : 2,
    maxPlayers: sessionInfo?.maxPlayers || 10,
    botsAllowed: gameType !== 'LASVEGAS',
    seriesAllowed: gameType !== 'LASVEGAS',
    internalRounds: gameType === 'LASVEGAS' ? 3 : 1,
  };
  const minPlayers = capabilities.minPlayers;
  const maxPlayers = Math.min(sessionInfo?.maxPlayers || capabilities.maxPlayers, capabilities.maxPlayers);
  const activePlayers = players.filter((player) => player.name?.trim());
  const playerCount = activePlayers.length;
  const readyPlayers = activePlayers.filter((player) => player.bot || player.ready);
  const allReady = readyPlayers.length === activePlayers.length && playerCount > 0;
  const canStart = Boolean(gameType && playerCount >= minPlayers && playerCount <= maxPlayers && allReady);
  const isOwner = Boolean(myUserId && sessionInfo?.ownerId === myUserId);

  useEffect(() => {
    if (preview) return;
    if (!token) navigate('/login');
  }, [token, navigate, preview]);

  useEffect(() => {
    if (preview) return undefined;
    if (!token) return undefined;
    let alive = true;
    setLoadingSession(true);
    getSession(sessionid, token)
      .then((info) => {
        if (!alive) return;
        setSessionInfo(info);
        setPlayers((info.players || []).map((player) => ({ name: player.nickname, bot: false, ready: true })));
      })
      .catch((requestError) => {
        if (alive) setError(requestError.message || 'Failed to load the room.');
      })
      .finally(() => { if (alive) setLoadingSession(false); });
    return () => { alive = false; };
  }, [sessionid, token, preview]);

  const addBot = () => {
    if (!capabilities.botsAllowed || playerCount >= maxPlayers) return;
    setPlayers((current) => [...current, { name: `Bot ${current.filter((player) => player.bot).length + 1}`, bot: true, ready: true }]);
  };

  const updatePlayer = (index, patch) => {
    setPlayers((current) => current.map((player, playerIndex) => playerIndex === index ? { ...player, ...patch } : player));
  };

  const copyInvite = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(sessionid);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setError('Copy failed. Select the session code and copy it manually.');
    }
  }, [sessionid]);

  const enterGame = useCallback((payload, totalRounds = rounds) => {
    const route = gameType === 'UNO'
      ? `/unoplayscreen/${sessionid}`
      : gameType === 'LASVEGAS'
        ? `/lasvegasplayscreen/${sessionid}`
        : `/dvcplayscreen/${sessionid}`;
    navigate(route, { state: { gameId: payload.gameId, roundIndex: payload.roundIndex, myPlayerId: payload.myPlayerId, players: payload.players, view: payload.view, totalRounds, results: [] } });
  }, [gameType, navigate, rounds, sessionid]);

  const startGame = async () => {
    if (!canStart || starting || !isOwner) return;
    setStarting(true);
    setError('');
    try {
      const response = await startFirstGame(sessionid, { rounds, players }, token);
      enterGame(response, rounds);
    } catch (requestError) {
      setError(requestError.message || 'Failed to start the game.');
    } finally {
      setStarting(false);
    }
  };

  useEffect(() => {
    if (preview) return undefined;
    if (!token) return undefined;
    let client;
    let active = true;
    const userTopic = myUserId ? `/topic/lobby/${sessionid}/${myUserId}` : null;
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
          client.subscribe(`/topic/lobby/${sessionid}`, (message) => {
            try {
              const payload = JSON.parse(message.body);
              setSessionInfo(payload);
              setPlayers((payload.players || []).map((player) => ({ name: player.nickname, bot: false, ready: true })));
            } catch {
              setError('A room update could not be read.');
            }
          });
          if (userTopic) {
            client.subscribe(userTopic, (message) => {
              try {
                enterGame(JSON.parse(message.body), capabilities.seriesAllowed ? rounds : 1);
              } catch {
                setError('The game started, but its launch message was invalid.');
              }
            });
          }
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
  }, [capabilities.seriesAllowed, enterGame, myUserId, rounds, sessionid, token, preview]);

  return (
    <PageContainer theme={gameType === 'UNO' ? 'uno' : gameType === 'DAVINCI' ? 'dvc' : gameType === 'LASVEGAS' ? 'vegas' : 'neutral'}>
      <div className="arcade-dashboard-layout">
        <header className="arcade-dashboard-header">
          <div>
            <p className="arcade-eyebrow">Waiting room // {gameType || 'Loading cabinet'}</p>
            <h1 className="arcade-title">Assemble your table</h1>
            <p className="arcade-copy mt-3">Share the code, check the seats, then let the host launch the match.</p>
          </div>
          <ConnectionBadge state={connectionState} />
        </header>

        <ArcadePanel quiet className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <p className="arcade-eyebrow">Invite code</p>
            <p className="arcade-code text-xl font-bold" title={sessionid}>{sessionid}</p>
          </div>
          <div className="arcade-actions">
            <ArcadeBadge tone={copied ? 'success' : 'muted'}>{copied ? 'Copied' : `${playerCount}/${maxPlayers} seated`}</ArcadeBadge>
            <ArcadeButton variant="secondary" onClick={copyInvite}>Copy invite</ArcadeButton>
          </div>
        </ArcadePanel>

        <div className="arcade-lobby-grid">
          <ArcadePanel aria-labelledby="players-title">
            <div className="flex items-center justify-between gap-3 mb-5">
              <div>
                <p className="arcade-eyebrow">Player select</p>
                <h2 id="players-title" className="text-xl font-bold">Seats</h2>
              </div>
              <ArcadeBadge tone={allReady ? 'success' : 'warning'}>{readyPlayers.length}/{playerCount} ready</ArcadeBadge>
            </div>
            <div className="arcade-player-list">
              {players.map((player, index) => (
                <div className="arcade-player-row" key={`${player.name}-${index}`}>
                  <PlayerSeat className="arcade-lobby-seat" name={player.name || `Player ${index + 1}`} index={index} meta={player.bot ? 'CPU' : 'Human'} />
                  <label className="arcade-field arcade-lobby-name min-w-0">
                    <span className="sr-only">Player {index + 1} name</span>
                    <input className="arcade-field__control" value={player.name} onChange={(event) => updatePlayer(index, { name: event.target.value })} disabled={player.bot} />
                  </label>
                  <label className="arcade-lobby-ready flex items-center gap-2 text-xs">
                    <span className="sr-only">Ready</span>
                    <input className="arcade-toggle" type="checkbox" checked={player.ready} onChange={(event) => updatePlayer(index, { ready: event.target.checked })} disabled={player.bot} />
                  </label>
                  {player.bot && <ArcadeButton className="arcade-lobby-remove" variant="ghost" size="small" onClick={() => setPlayers((current) => current.filter((_, playerIndex) => playerIndex !== index))} aria-label={`Remove ${player.name}`}>Remove</ArcadeButton>}
                </div>
              ))}
              {!players.length && !loadingSession && <div className="arcade-empty">No players have joined yet.</div>}
              {capabilities.botsAllowed && (
                <ArcadeButton variant="ghost" block onClick={addBot} disabled={playerCount >= maxPlayers}>+ Add bot</ArcadeButton>
              )}
            </div>
          </ArcadePanel>

          <ArcadePanel quiet aria-labelledby="room-console-title">
            <p className="arcade-eyebrow">Room console</p>
            <h2 id="room-console-title" className="text-xl font-bold">Match setup</h2>
            <div className="arcade-form-stack mt-6">
              {capabilities.seriesAllowed ? (
                <ArcadeSelect label="Rounds" value={rounds} onChange={(event) => setRounds(Number(event.target.value))}>
                  {Array.from({ length: 10 }, (_, index) => index + 1).map((round) => <option key={round} value={round}>{round}</option>)}
                </ArcadeSelect>
              ) : (
                <div className="arcade-status arcade-status--info" role="status">
                  1 platform game / {capabilities.internalRounds} casino rounds
                </div>
              )}
              <div className="arcade-copy text-sm">
                <p>Game: <strong className="arcade-accent">{gameType || 'Loading'}</strong></p>
                <p>Capacity: {minPlayers}-{maxPlayers}</p>
                <p>Bots: {capabilities.botsAllowed ? 'Allowed' : 'Not available'}</p>
                <p>Host control: {isOwner ? 'You are the host' : 'Waiting for the host'}</p>
              </div>
              {loadingSession && <div className="arcade-status arcade-status--info" role="status">Loading room state...</div>}
              <ErrorPopup message={error} />
              <ArcadeButton loading={starting} disabled={!canStart || !isOwner} onClick={startGame}>Start game</ArcadeButton>
              {!isOwner && <p className="arcade-field__hint">Only the host can start after every seat is ready.</p>}
              <ArcadeButton variant="ghost" onClick={() => navigate(-1)}>Back to dashboard</ArcadeButton>
            </div>
          </ArcadePanel>
        </div>
      </div>
    </PageContainer>
  );
}
