import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';

export default function SessionSummary() {
  const { sessionid } = useParams();
  const nav = useNavigate();
  const [data, setData] = useState(null); // { totalRounds, results:[{round,winnerId,turns}], playersMeta }

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(`uno-results-${sessionid}`);
      if (raw) setData(JSON.parse(raw));
    } catch { /* ignore */ }
  }, [sessionid]);

  const playersMeta = data?.playersMeta || [];
  const idToName = useMemo(() => {
    const m = new Map();
    playersMeta.forEach(p => m.set(p.playerId, p.name || p.playerId));
    return m;
  }, [playersMeta]);

  const ranking = useMemo(() => {
    if (!data) return [];
    const winCount = new Map();
    data.results.forEach(r => winCount.set(r.winnerId, (winCount.get(r.winnerId) || 0) + 1));
    return [...winCount.entries()]
      .sort((a,b)=> b[1]-a[1] || a[0].localeCompare(b[0]))
      .map(([playerId, wins]) => ({ playerId, wins, name: idToName.get(playerId) || playerId }));
  }, [data, idToName]);

  const top3 = ranking.slice(0,3);

  const podium = useMemo(() => {
    if (top3.length === 0) return null;
    // Arrange as [second, first, third] for visual left-center-right; center tallest
    const first = top3[0];
    const second = top3[1];
    const third = top3[2];
    return [second, first, third];
  }, [top3]);

  if (!data) {
    return (
      <PageContainer>
        <CardContainer className="max-w-xl w-full text-center">
          <h2 className="text-xl font-semibold mb-4">Session Summary</h2>
          <div className="text-sm opacity-70">No data found.</div>
          <SubmitButton type="button" className="btn-secondary mt-4" onClick={()=>nav('/dashboard')}>Return to Dashboard</SubmitButton>
        </CardContainer>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <CardContainer className="max-w-2xl w-full">
        <h2 className="text-xl font-semibold mb-2">Session Summary</h2>
        <div className="text-xs opacity-70 mb-4">Session: <span className="font-mono font-semibold">{sessionid}</span></div>
        <div className="mb-4 text-sm">Total Games: {data.totalRounds}</div>
        <h3 className="font-semibold mb-2">Game Results</h3>
        <div className="overflow-x-auto mb-6">
          <table className="table table-zebra w-full text-xs">
            <thead><tr><th>Round</th><th>Winner</th><th>Turns</th></tr></thead>
            <tbody>
              {[...data.results].sort((a,b)=>a.round-b.round).map(r => {
                const name = r.winnerName || idToName.get(r.winnerId) || r.winnerId;
                return (
                  <tr key={r.round}>
                    <td>{r.round}</td>
                    <td>{name}</td>
                    <td>{r.turns}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <h3 className="font-semibold mb-2">Podium (Top 3)</h3>
        {podium ? (
          <div className="flex items-end justify-center gap-4 mb-6 mt-4">
            {podium.map((p, idx) => {
              if (!p) return <div key={idx} className="w-20" />;
              const order = idx === 1 ? 'first' : (idx === 0 ? 'second' : 'third');
              const heights = { first: 'h-40', second: 'h-32', third: 'h-28' };
              const bg = { first: 'bg-yellow-400', second: 'bg-gray-300', third: 'bg-amber-700 text-amber-100' };
              return (
                <div key={p.playerId} className="flex flex-col items-center w-24">
                  <div className="text-xs font-semibold mb-2 whitespace-nowrap max-w-full text-center overflow-hidden text-ellipsis" title={p.name}>{p.name}</div>
                  <div className={`flex flex-col items-center justify-end w-full rounded-t-md ${bg[order]} ${heights[order]} relative shadow`}> 
                    <div className="text-[10px] font-bold mb-1">{order.toUpperCase()}</div>
                    <div className="text-sm font-bold">{p.wins}</div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="text-xs opacity-60 mb-6">No wins</div>
        )}
        {ranking.length > 3 && (
          <div className="mb-6 text-xs">
            <div className="font-semibold mb-1">Other Players</div>
            <ul className="space-y-1">
              {ranking.slice(3).map(r => (
                <li key={r.playerId}>{r.name}: {r.wins}</li>
              ))}
            </ul>
          </div>
        )}
        <SubmitButton type="button" className="btn-secondary" onClick={()=>nav('/dashboard')}>Return to Dashboard</SubmitButton>
      </CardContainer>
    </PageContainer>
  );
}
