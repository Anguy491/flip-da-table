import React from 'react';

export function InfoPanel({ deckRemaining, currentPlayerId, roundIndex, awaiting }) {
  return (
    <div className="dvc-info text-xs p-2 border rounded flex flex-col gap-1">
      <div><span className="font-semibold">Deck:</span> {deckRemaining}</div>
      <div><span className="font-semibold">Current:</span> {currentPlayerId}</div>
      <div><span className="font-semibold">Round:</span> {roundIndex}</div>
      <div><span className="font-semibold">Awaiting:</span> {awaiting}</div>
    </div>
  );
}
