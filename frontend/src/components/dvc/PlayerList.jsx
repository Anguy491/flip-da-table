import React from 'react';
import { PlayerRow } from './PlayerRow';
import { parseCard } from './parseCard';

// Renders only opponents (excludes perspective player's own row)
export function PlayerList({ playerViews, currentPlayerId, myPlayerId, clickable=false, onOpponentCardClick }) {
  const opponents = playerViews.filter(p => p.playerId !== myPlayerId);
  return (
    <div className="dvc-players flex flex-col gap-1">
      {opponents.map((p,i)=> (
        <PlayerRow
          key={p.playerId}
          pv={p}
          index={i}
          currentPlayerId={currentPlayerId}
          parseCardFn={parseCard}
          clickable={clickable}
          onCardClick={(cardIndex)=> onOpponentCardClick && onOpponentCardClick(p.playerId, cardIndex)}
        />
      ))}
    </div>
  );
}
