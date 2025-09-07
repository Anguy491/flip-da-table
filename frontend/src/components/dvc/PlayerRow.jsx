import React from 'react';
import { CardStrip } from './CardStrip';

export function PlayerRow({ pv, index, currentPlayerId, parseCardFn, clickable=false, onCardClick }) {
  const parsed = pv.cards.map(parseCardFn);
  return (
    <div className={`dvc-player-row flex items-center gap-2 p-1 rounded ${pv.playerId===currentPlayerId?'bg-primary/10':''}`} data-testid={`player-${index}`}>
      <div className="dvc-avatar w-8 h-8 rounded-full bg-base-300 flex items-center justify-center text-[10px] font-bold">{index+1}</div>
      <div className="flex flex-col flex-1">
        <div className="text-xs font-mono flex items-center gap-2"><span>{pv.playerId}</span></div>
  <CardStrip cards={parsed} clickable={clickable} onCardClick={onCardClick} />
      </div>
      <div className="text-[10px] opacity-60 w-10 text-center">{pv.hiddenCount>0?`H:${pv.hiddenCount}`:'All'}</div>
    </div>
  );
}
