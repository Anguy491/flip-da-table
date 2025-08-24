import React from 'react';

export default function UnoPlayerStrip({ players, currentPlayerId, viewerId }) {
  return (
    <div className="flex flex-wrap gap-2 justify-center">
      {players.map(p => (
        <div key={p.playerId}
             className={`px-3 py-2 rounded border text-xs flex flex-col items-center gap-1 min-w-[90px] ${p.playerId===currentPlayerId ? 'border-primary bg-base-200' : 'border-base-300'}`}
        >
          <span className="font-semibold truncate max-w-[70px]" title={p.playerId}>{p.bot ? `🤖 ${p.playerId.slice(0,4)}` : p.playerId.slice(0,6)}</span>
          <span className="font-mono text-sm">{p.handSize}🂠</span>
          {p.isWinner && <span className="badge badge-success badge-xs">WIN</span>}
          {p.playerId===viewerId && <span className="badge badge-info badge-xs">YOU</span>}
        </div>
      ))}
    </div>
  );
}
