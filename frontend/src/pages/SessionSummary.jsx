import { useContext, useEffect, useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import {
  ArcadeBadge,
  ArcadeButton,
  ArcadePanel,
  Scoreboard,
} from '../components/arcade/ArcadeUI';
import { readSessionResults } from '../utils/sessionResults';
import { getLatestGame } from '../api/sessions';
import { AuthContext } from '../context/auth-context';

export default function SessionSummary({ previewData = null, previewSessionId = null }) {
  const { sessionid: routeSessionId } = useParams();
  const sessionid = previewSessionId || routeSessionId;
  const navigate = useNavigate();
  const { token } = useContext(AuthContext);
  const [data, setData] = useState(previewData);

  useEffect(() => {
    if (previewData) {
      setData(previewData);
      return;
    }
    const stored = readSessionResults(sessionid);
    if (stored) {
      setData(stored);
      return undefined;
    }
    if (!token) return undefined;
    let active = true;
    getLatestGame(sessionid, token)
      .then((latest) => {
        if (!active || latest.gameType !== 'LASVEGAS' || latest.view?.phase !== 'FINISHED') return;
        setData({
          gameType: 'LASVEGAS',
          totalRounds: latest.view.totalRounds,
          vegasResults: latest.view.results || [],
        });
      })
      .catch(() => {});
    return () => { active = false; };
  }, [sessionid, previewData, token]);

  const ranking = useMemo(() => {
    if (!data) return [];
    const idToName = new Map((data.playersMeta || []).map((player) => [player.playerId, player.name || player.playerId]));
    const winCount = new Map();
    for (const result of data.results || []) winCount.set(result.winnerId, (winCount.get(result.winnerId) || 0) + 1);
    return [...winCount.entries()]
      .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
      .map(([playerId, wins]) => ({ playerId, wins, name: idToName.get(playerId) || playerId }));
  }, [data]);

  if (!data) {
    return (
      <PageContainer>
        <ArcadePanel className="max-w-2xl mx-auto text-center">
          <p className="arcade-eyebrow">High score table</p>
          <h1 className="arcade-title">No results saved</h1>
          <p className="arcade-copy mt-5">Finish a table session to populate this cabinet.</p>
          <ArcadeButton className="mt-7" onClick={() => navigate('/dashboard')}>Return to dashboard</ArcadeButton>
        </ArcadePanel>
      </PageContainer>
    );
  }

  const isLasVegas = data.gameType === 'LASVEGAS';
  const topThree = ranking.slice(0, 3);
  const podium = [topThree[1], topThree[0], topThree[2]];
  const placement = ['second', 'first', 'third'];
  const columns = [
    { key: 'round', label: 'Round' },
    { key: 'winner', label: 'Winner' },
    { key: 'turns', label: 'Turns' },
  ];
  const nameById = new Map((data.playersMeta || []).map((player) => [player.playerId, player.name || player.playerId]));
  const rows = [...(data.results || [])].sort((left, right) => left.round - right.round).map((result) => ({
    round: String(result.round).padStart(2, '0'),
    winner: result.winnerName || nameById.get(result.winnerId) || result.winnerId,
    turns: result.turns,
  }));

  return (
    <PageContainer theme={data.gameType === 'DAVINCI' ? 'dvc' : isLasVegas ? 'vegas' : 'uno'}>
      <div className="arcade-dashboard-layout">
        <header className="arcade-dashboard-header">
          <div>
            <p className="arcade-eyebrow">Session complete // high scores saved</p>
            <h1 className="arcade-title">Final scoreboard</h1>
            <p className="arcade-copy mt-3">Session <span className="arcade-code arcade-accent">{sessionid}</span></p>
          </div>
          <ArcadeBadge tone="success">{isLasVegas ? `${data.totalRounds} casino rounds` : `${data.totalRounds} games`}</ArcadeBadge>
        </header>

        {isLasVegas && (
          <ArcadePanel aria-labelledby="vegas-summary-title">
            <p className="arcade-eyebrow">Top assets // ties share victory</p>
            <h2 id="vegas-summary-title" className="text-xl font-bold mb-5">
              {(data.vegasResults || []).filter((player) => player.winner).map((player) => player.name).join(' & ') || 'Final standings'}
            </h2>
            <Scoreboard
              columns={[
                { key: 'rank', label: 'Rank', render: (row) => `#${row.rank}${row.winner ? ' WIN' : ''}` },
                { key: 'name', label: 'Player' },
                { key: 'cashTotal', label: 'Cash', render: (row) => `$${row.cashTotal.toLocaleString('en-US')}` },
                { key: 'chips', label: 'Chips' },
                { key: 'tieBreakCount', label: 'Cards + chips' },
                { key: 'totalAssets', label: 'Total', render: (row) => `$${row.totalAssets.toLocaleString('en-US')}` },
              ]}
              rows={data.vegasResults || []}
              getRowKey={(row) => row.playerId}
            />
          </ArcadePanel>
        )}

        {!isLasVegas && podium.some(Boolean) && (
          <ArcadePanel aria-labelledby="podium-title">
            <p className="arcade-eyebrow">Top players</p>
            <h2 id="podium-title" className="text-xl font-bold">Cabinet champions</h2>
            <div className="arcade-podium">
              {podium.map((player, index) => player ? (
                <div className={`arcade-podium__place arcade-podium__place--${placement[index]}`} key={player.playerId}>
                  <span className="arcade-label text-[0.55rem]">{placement[index]}</span>
                  <strong className="mt-3 break-words">{player.name}</strong>
                  <span className="arcade-score arcade-accent mt-4">{player.wins}</span>
                  <span className="arcade-muted text-xs">wins</span>
                </div>
              ) : <div className="arcade-podium__place arcade-panel--quiet" key={placement[index]}>---</div>)}
            </div>
          </ArcadePanel>
        )}

        {!isLasVegas && <ArcadePanel aria-labelledby="rounds-title">
          <p className="arcade-eyebrow">Round archive</p>
          <h2 id="rounds-title" className="text-xl font-bold mb-5">Game results</h2>
          <Scoreboard columns={columns} rows={rows} getRowKey={(row) => row.round} />
        </ArcadePanel>}

        {!isLasVegas && ranking.length > 3 && (
          <ArcadePanel quiet>
            <p className="arcade-eyebrow">Remaining players</p>
            <div className="arcade-actions">
              {ranking.slice(3).map((player) => <ArcadeBadge key={player.playerId} tone="muted">{player.name}: {player.wins}</ArcadeBadge>)}
            </div>
          </ArcadePanel>
        )}

        <div className="arcade-actions">
          <ArcadeButton onClick={() => navigate('/dashboard')}>Return to dashboard</ArcadeButton>
        </div>
      </div>
    </PageContainer>
  );
}
