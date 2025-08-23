import { useParams, useNavigate } from 'react-router-dom';
import { useContext, useEffect, useState, useCallback } from 'react';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import { startFirstGame } from '../api/sessions';

export default function Lobby() {
  const { sessionid } = useParams();
  const nav = useNavigate();
  const { token } = useContext(AuthContext);
  const [players, setPlayers] = useState(() => [
    { name: 'Host', bot: false, ready: false },
  ]);
  const [rounds, setRounds] = useState(1);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');
  const gameType = 'DAVINCI'; // 目前仅支持 DaVinci 示例

  const playerCount = players.filter(p => p.name && p.name.trim()).length;
  const canStart = gameType === 'DAVINCI' ? playerCount >= 2 && playerCount <= 4 : playerCount >= 1;

  useEffect(() => {
    if (!token) nav('/login');
  }, [token, nav]);

  const addBot = () => {
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
      nav(`/playscreen/${sessionid}`, { state: { gameId: resp.gameId, roundIndex: resp.roundIndex } });
    } catch (e) {
      setError(e.message || 'Failed to start');
    } finally {
      setStarting(false);
    }
  };

  return (
    <PageContainer>
      <CardContainer className="max-w-3xl w-full">
        <h2 className="text-xl font-semibold">Lobby</h2>
        <div className="text-sm">Session: <span className="font-mono font-bold">{sessionid}</span></div>

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
                      disabled={p.bot} // 电脑玩家默认已准备且不可修改
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
                  <button type="button" className="btn btn-outline btn-sm w-full" onClick={addBot}>+ Add Bot</button>
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
            <div className="text-xs opacity-70">Players (DaVinci): need 2-4. Current: {playerCount}</div>
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
