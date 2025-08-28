import React from 'react';

export default function GameOverModal({ open, winnerName, winnerId, turns, onClose, onNext, isLast, onSummary }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black/60 z-50">
      <div className="bg-base-100 rounded-lg shadow-xl p-6 w-full max-w-sm flex flex-col gap-4">
        <h3 className="text-lg font-bold text-center">Game Over</h3>
        <div className="text-center text-sm">
          <div className="font-semibold">Winner</div>
          <div className="text-primary font-mono text-base">{winnerName || winnerId}</div>
          <div className="mt-2 opacity-70">Turns: {turns}</div>
        </div>
        <div className="flex justify-between mt-2">
          <button type="button" className="btn btn-sm btn-outline" onClick={onClose}>Return to Dashboard</button>
          {isLast ? (
            <button type="button" className="btn btn-sm btn-primary" onClick={onSummary}>Get Summary</button>
          ) : (
            <button type="button" className="btn btn-sm btn-primary" onClick={onNext}>Next Game</button>
          )}
        </div>
      </div>
    </div>
  );
}
