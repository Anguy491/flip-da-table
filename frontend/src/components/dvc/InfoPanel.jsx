import React from 'react';

export function InfoPanel({ deckRemaining, deckBlackRemaining=0, deckWhiteRemaining=0, currentPlayerId, roundIndex, awaiting }) {
  return (
    <div className="dvc-info text-xs p-2 border rounded flex flex-col gap-1">
  <div>
    <span className="font-semibold">Deck:</span> {deckRemaining}
    <span className="opacity-70"> (
      <span className="inline-block w-2.5 h-2.5 bg-black align-middle ml-1 mr-1" aria-label="black" title="black"></span>
      {deckBlackRemaining}
      {' '}|
      <span className="inline-block w-2.5 h-2.5 bg-white border border-gray-400 align-middle mx-1" aria-label="white" title="white"></span>
      {deckWhiteRemaining}
    )</span>
  </div>
      <div><span className="font-semibold">Current:</span> {currentPlayerId}</div>
      <div><span className="font-semibold">Round:</span> {roundIndex}</div>
      <div><span className="font-semibold">Awaiting:</span> {awaiting}</div>
    </div>
  );
}
