import React from 'react';
import UnoCard from './UnoCard';

export default function UnoHand({ hand, playableCards, onPlay, myTurn, mustChooseColor }) {
  return (
    <div className="flex flex-wrap gap-2 justify-center">
      {hand.map((c, idx) => {
        const playable = playableCards.some(pc => pc === c || (pc.color === c.color && pc.value === c.value));
        return (
          <UnoCard key={idx} card={c} onClick={() => onPlay(c)} disabled={!myTurn || mustChooseColor || !playable} />
        );
      })}
      {hand.length === 0 && <div className="text-sm opacity-60">(No Cards)</div>}
    </div>
  );
}
