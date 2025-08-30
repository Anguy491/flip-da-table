import React from 'react';
import UnoCard from '../../uno/UnoCard';
import SubmitButton from '../../SubmitButton';

/**
 * HandArea renders overlapping hand + draw button.
 * props: hand [{id,color,value,type}], playableIds Set(card.id) (optional), disabled, onPlay, onDraw, pendingDraw
 */
export default function HandArea({ hand = [], playableIds = new Set(), disabled, onPlay, onDraw, pendingDraw }) {
  const overlapRatio = 0.7; // show 30%

  return (
    <div className={`relative w-full h-full flex flex-col`}>
      <div className={`flex-1 flex items-end justify-center overflow-x-auto px-4 pb-2 ${disabled ? 'opacity-50 pointer-events-none' : ''}`}>
        <div className="relative flex items-end h-40">{/* hand visual height */}
          {hand.map((card, idx) => {
            const playable = playableIds.has(card.id) || playableIds.has(card);
            const width = 64; // w-16
            const ml = idx === 0 ? 0 : -width * overlapRatio;
            return (
              <div
                key={card.id || idx}
                className="card-wrapper group"
                style={{ marginLeft: ml }}
              >
                <UnoCard
                  card={card}
                  onClick={() => playable && onPlay?.(card.id || card)}
                  disabled={!playable || disabled}
                />
              </div>
            );
          })}
          {hand.length === 0 && <div className="text-xs opacity-60">(Empty)</div>}
        </div>
        <div className="flex items-center pl-4">
          <SubmitButton type="button" className="btn-secondary" onClick={onDraw} disabled={disabled}>{pendingDraw>0?`Draw ${pendingDraw}`:'Draw'}</SubmitButton>
        </div>
      </div>
      <style>{`
        .card-wrapper { position: relative; transition: transform .15s ease, margin .15s ease; }
        .card-wrapper:hover { z-index: 20; transform: translateY(-10px) scale(1.05); margin-left: 0 !important; }
        .card-wrapper:hover + .card-wrapper { /* allow reveal of hovered card without gap collapse */ }
      `}</style>
    </div>
  );
}
