import { useParams, useNavigate } from 'react-router-dom';
import { useContext, useEffect, useState, useCallback } from 'react';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import { startFirstGame, getSession } from '../api/sessions';

export default function Lobby() {
  const { sessionid } = useParams();
  const nav = useNavigate();
  const { token } = useContext(AuthContext);
  const [players, setPlayers] = useState(() => [
    { name: 'Host', bot: false, ready: false },
  ]);
  const [sessionInfo, setSessionInfo] = useState(null); // { id, ownerId, gameType, maxPlayers }
  const [loadingSession, setLoadingSession] = useState(true);
  const [rounds, setRounds] = useState(1);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');
  const gameType = sessionInfo?.gameType?.toUpperCase();
  const maxPlayers = sessionInfo?.maxPlayers || 10;

  const playerCount = players.filter(p => p.name && p.name.trim()).length;

  const canStart = gameType ? (playerCount >= 2 && playerCount <= maxPlayers) : false;

  useEffect(() => {
    if (!token) nav('/login');
  }, [token, nav]);

  useEffect(() => {
    if (!token) return;
    let alive = true;
    (async () => {
      setLoadingSession(true);
      try {
        const info = await getSession(sessionid, token);
        if (alive) setSessionInfo(info);
      } catch (e) {
        setError(e.message || 'Failed to load session');
      } finally {
        if (alive) setLoadingSession(false);
      }
    })();
    return () => { alive = false; };
  }, [sessionid, token]);

  const addBot = () => {
    if (playerCount >= maxPlayers) return;
    setPlayers(prev => [...prev, { name: `Bot${prev.length}`, bot: true, ready: true }]);
  };

  const updatePlayer = (idx, patch) => {
    setPlayers(prev => prev.map((p, i) => i === idx ? { ...p, ...patch } : p));
  };

  const copyInvite = useCallback(() => {
    navigator.clipboard.writeText(sessionid).catch(() => {
      // ignore
    });
  }, [sessionid]);

  const startGame = async () => {
    if (!canStart || starting) return;
    setStarting(true); setError('');
    try {
  const resp = await startFirstGame(sessionid, { rounds, players }, token);
  nav(`/playscreen/${sessionid}`, { state: { gameId: resp.gameId, roundIndex: resp.roundIndex, playerId: resp.myPlayerId, players: resp.players } });
    } catch (e) {
      setError(e.message || 'Failed to start');
    } finally {
      setStarting(false);
    }
  };

  return (
    <PageContainer>
      <CardContainer className="max-w-3xl w-full">
        <h2 className="text-xl font-semibold">Lobby {gameType ? `- ${gameType}` : ''}</h2>
        <div className="text-sm">Session: <span className="font-mono font-bold">{sessionid}</span></div>
        {loadingSession && <div className="text-xs opacity-70">Loading session...</div>}
        {sessionInfo && (
          <div className="text-xs opacity-70 flex gap-4 flex-wrap">
            <span>Game Type: {sessionInfo.gameType}</span>
            <span>Max Players: {maxPlayers}</span>
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="table table-zebra w-full mt-4">
            <thead>
              <tr>
                <th>#</th>
                <th>Player Name</th>
                <th>Type</th>
                <th>Ready</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {players.map((p, i) => (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td>
                    {p.bot ? (
                      <span className="font-mono">{p.name}</span>
                    ) : (
                      <input
                        className="input input-bordered input-sm w-full"
                        value={p.name}
                        onChange={e => updatePlayer(i, { name: e.target.value })}
                      />
                    )}
                  </td>
                  <td>{p.bot ? 'Bot' : 'Human'}</td>
                  <td>
                    <input
                      type="checkbox"
                      className="toggle toggle-sm"
                      checked={p.ready}
                      onChange={e => updatePlayer(i, { ready: e.target.checked })}
                      disabled={p.bot}
                    />
                  </td>
                  <td>
                    {p.bot ? (
                      <button
                        type="button"
                        className="btn btn-ghost btn-xs text-error"
                        onClick={() => setPlayers(prev => prev.filter((_, idx) => idx !== i))}
                        aria-label="Remove bot"
                      >✕</button>
                    ) : null}
                  </td>
                </tr>
              ))}
              <tr>
                <td colSpan={5}>
                  <button
                    type="button"
                    className="btn btn-outline btn-sm w-full"
                    onClick={addBot}
                    disabled={playerCount >= maxPlayers}
                    title={playerCount >= maxPlayers ? 'Reached max players' : ''}
                  >+ Add Bot</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="flex flex-col gap-4 mt-4">
          <div className="flex items-center gap-4 flex-wrap">
            <label className="flex items-center gap-2">Rounds
              <select className="select select-bordered select-sm" value={rounds} onChange={e => setRounds(Number(e.target.value))}>
                {Array.from({ length: 10 }, (_, i) => i + 1).map(n => <option key={n} value={n}>{n}</option>)}
              </select>
            </label>
            <div className="text-xs opacity-70">
              {gameType ? (
                <>Players need 2-{maxPlayers}. Current: {playerCount}</>
              ) : 'Loading rules...'}
            </div>
          </div>
          {error && <div className="alert alert-error py-2 px-3 text-sm">{error}</div>}
          <div className="flex justify-between items-center">
            <div className="flex gap-2">
              <button type="button" className="btn btn-secondary btn-sm" onClick={copyInvite}>Invite</button>
              <button type="button" className="btn btn-outline btn-sm" onClick={() => nav(-1)}>Back</button>
            </div>
            <SubmitButton type="button" disabled={!canStart || starting} onClick={startGame}>
              {starting ? 'Starting...' : 'Start Game'}
            </SubmitButton>
          </div>
        </div>
      </CardContainer>
    </PageContainer>
  );
}
