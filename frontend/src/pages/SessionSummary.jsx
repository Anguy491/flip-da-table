import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';

export default function SessionSummary() {
  const { sessionid } = useParams();
  const nav = useNavigate();
  const [data, setData] = useState(null); // { totalRounds, results:[{round,winnerId,winnerName,turns}] }

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(`uno-results-${sessionid}`);
      if (raw) setData(JSON.parse(raw));
    } catch { /* ignore */ }
  }, [sessionid]);

  const ranking = useMemo(() => {
    if (!data) return [];
    const map = new Map();
    data.results.forEach(r => { map.set(r.winnerId, (map.get(r.winnerId)||0)+1); });
    return [...map.entries()].sort((a,b)=>b[1]-a[1]).map(([pid, wins]) => ({ playerId: pid, wins }));
  }, [data]);

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
              {data.results.map(r => (
                <tr key={r.round}>
                  <td>{r.round}</td>
                  <td>{r.winnerName || r.winnerId}</td>
                  <td>{r.turns}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h3 className="font-semibold mb-2">Win Ranking</h3>
        <ul className="mb-6 text-sm">
          {ranking.map(r => <li key={r.playerId}>{r.playerId}: {r.wins}</li>)}
          {ranking.length === 0 && <li className="opacity-60">No wins</li>}
        </ul>
        <SubmitButton type="button" className="btn-secondary" onClick={()=>nav('/dashboard')}>Return to Dashboard</SubmitButton>
      </CardContainer>
    </PageContainer>
  );
}
