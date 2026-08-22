import { useEffect, useRef, useState } from 'react';
import { ArcadeBadge } from '../arcade/ArcadeUI';
import { CardStrip } from './CardStrip';
import { isArrangementValid } from './arrangement';

function tokenFor(card) {
  const prefix = card?.color === 'BLACK' ? 'B' : 'W';
  const value = card?.isJoker || card?.value === '-' ? '_' : String(card?.value ?? '');
  return `${prefix}${value}≤`;
}

export function MyHandPanel({ cards = [], draggable, onReorder, showValidity = false, publicTokens = new Set(), selectable = false, selectedIndex = null, onSelect }) {
  const valid = isArrangementValid(cards);
  const scrollerRef = useRef(null);
  const [overflowing, setOverflowing] = useState(false);

  useEffect(() => {
    const update = () => {
      const element = scrollerRef.current;
      setOverflowing(Boolean(element && element.scrollWidth > element.clientWidth));
    };
    update();
    window.addEventListener('resize', update);
    return () => window.removeEventListener('resize', update);
  }, [cards]);

  return (
    <div>
      <div className="flex items-center justify-between gap-3 mb-2">
        <h3 className="arcade-game-zone__title mb-0">Your code rack</h3>
        <div className="flex gap-2">
          {overflowing && <ArcadeBadge tone="muted">Scroll rack</ArcadeBadge>}
          {showValidity && <ArcadeBadge tone={valid ? 'success' : 'error'}>{valid ? 'Order valid' : 'Fix order'}</ArcadeBadge>}
        </div>
      </div>
      <div ref={scrollerRef} className="overflow-x-auto pb-2">
        <CardStrip
          cards={cards}
          draggable={draggable}
          onReorder={onReorder}
          clickable={selectable}
          canClick={(_, card) => !(card?.revealed && publicTokens.has(tokenFor(card)))}
          onCardClick={(index) => {
            if (selectable) onSelect?.(selectedIndex === index ? null : index);
          }}
          itemClassName={(index, card) => {
            const isPublic = card?.revealed && publicTokens.has(tokenFor(card));
            return [
              isPublic ? 'dvc-public-trapezoid' : '',
              selectable && selectedIndex === index && !isPublic ? 'dvc-card-hit--selected' : '',
            ].filter(Boolean).join(' ');
          }}
        />
      </div>
    </div>
  );
}
