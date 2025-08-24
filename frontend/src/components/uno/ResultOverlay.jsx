import React from 'react';

export default function ResultOverlay({ players, open, onClose }) {
  if (!open) return null;
  const winners = players.filter(p => p.isWinner);
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-40 p-4">
      <div className="bg-base-100 p-6 rounded shadow-xl w-full max-w-sm space-y-4 text-center">
        <h3 className="text-xl font-semibold">Game Finished</h3>
        {winners.length ? (
          <div className="space-y-2">
            <div className="font-mono text-sm">Winner{winners.length>1?'s':''}:</div>
            <ul className="flex flex-col gap-1">
              {winners.map(w => <li key={w.playerId} className="badge badge-success whitespace-nowrap">{w.playerId.slice(0,6)} {w.bot && '🤖'}</li>)}
            </ul>
          </div>
        ) : <div>No winner captured.</div>}
        <button type="button" className="btn btn-primary w-full" onClick={onClose}>Back</button>
      </div>
    </div>
  );
}
