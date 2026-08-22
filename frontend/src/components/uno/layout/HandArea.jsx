import UnoCard from '../UnoCard';
import { ArcadeButton } from '../../arcade/ArcadeUI';

export default function HandArea({ hand = [], playableIds = new Set(), disabled, onPlay, onDraw, pendingDraw, sending = false }) {
  return (
    <div className="flex flex-col md:flex-row items-end gap-4">
      <div className="flex-1 min-w-0 overflow-x-auto" aria-label="Your hand">
        <div className="uno-hand">
          {hand.map((card, index) => {
            const playable = playableIds.has(card.id) || playableIds.has(card);
            return (
              <div key={card.id || `${card.color}-${card.value}-${index}`} className="uno-hand__card" style={{ marginLeft: index === 0 ? 0 : -40 }}>
                <UnoCard card={card} onClick={(selected) => onPlay?.(selected)} disabled={!playable || disabled} />
              </div>
            );
          })}
          {!hand.length && <div className="arcade-empty min-h-36">Your hand is empty.</div>}
        </div>
      </div>
      <ArcadeButton variant="secondary" loading={sending} onClick={onDraw} disabled={disabled}>{pendingDraw > 0 ? `Draw ${pendingDraw}` : 'Draw card'}</ArcadeButton>
    </div>
  );
}
