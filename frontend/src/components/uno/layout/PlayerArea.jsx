import React from 'react';

/**
 * PlayerArea: horizontally distributes player capsules; highlights current.
 * props.players: [{ id, name, avatarUrl, handCount }]
 */
export default function PlayerArea({ players = [], currentPlayerId }) {
  return (
    <div className="flex w-full h-full items-stretch justify-center gap-2 px-2 overflow-x-auto">{/* allow horizontal scroll if many */}
      {players.map(p => {
        const isCurrent = p.id === currentPlayerId;
        return (
          <div
            key={p.id}
            data-player-id={p.id}
            className={`flex-1 min-w-[90px] flex flex-col items-center justify-center rounded-md border text-xs relative py-1 select-none transition-colors ${isCurrent ? 'border-primary bg-primary/10 shadow-inner' : 'border-base-300 bg-base-200/40'}`}
          >
            <div className="flex flex-col items-center gap-1">
              <div className="w-10 h-10 rounded-full bg-base-300 overflow-hidden flex items-center justify-center text-[10px] font-semibold">
                {p.avatarUrl ? <img src={p.avatarUrl} alt={p.name} className="w-full h-full object-cover" /> : (p.name || p.id).slice(0,2).toUpperCase()}
              </div>
              <span className={`font-medium truncate max-w-[80px] ${isCurrent ? 'text-primary' : ''}`} title={p.name || p.id}>{p.name || p.id}</span>
            </div>
            <span className="absolute top-1 right-1 badge badge-xs badge-neutral font-mono" title="Hand Size">{p.handCount}</span>
          </div>
        );
      })}
      {players.length === 0 && <div className="text-xs opacity-60">No Players</div>}
    </div>
  );
}
