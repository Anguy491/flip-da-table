import { CardTile } from './CardTile';
import { parseCard } from './parseCard';

export function CardStrip({ cards = [], draggable = false, onReorder, clickable = false, onCardClick, itemClassName, canClick }) {
  const handleDrop = (event, targetIndex) => {
    if (!draggable) return;
    event.preventDefault();
    const sourceIndex = Number(event.dataTransfer.getData('text/plain'));
    if (!Number.isNaN(sourceIndex) && sourceIndex !== targetIndex) onReorder?.(sourceIndex, targetIndex);
  };

  return (
    <div className="dvc-card-strip">
      {cards.map((raw, index) => {
        const card = typeof raw === 'string' ? parseCard(raw) : raw;
        const allowed = typeof canClick === 'function' ? Boolean(canClick(index, card)) : !card.revealed;
        const interactive = clickable && allowed;
        return (
          <button
            type="button"
            key={`${card.color}-${card.value}-${index}`}
            draggable={draggable}
            className={`dvc-card-hit ${typeof itemClassName === 'function' ? itemClassName(index, card) : itemClassName || ''}`}
            data-clickable={interactive ? 'true' : 'false'}
            disabled={!interactive && !draggable}
            onClick={() => { if (interactive) onCardClick?.(index); }}
            onKeyDown={(event) => {
              if (!draggable) return;
              if (event.key === 'ArrowLeft' && index > 0) {
                event.preventDefault();
                onReorder?.(index, index - 1);
              }
              if (event.key === 'ArrowRight' && index < cards.length - 1) {
                event.preventDefault();
                onReorder?.(index, index + 1);
              }
            }}
            onDragStart={(event) => {
              if (!draggable) return;
              event.dataTransfer.effectAllowed = 'move';
              event.dataTransfer.setData('text/plain', String(index));
            }}
            onDragOver={(event) => { if (draggable) event.preventDefault(); }}
            onDrop={(event) => handleDrop(event, index)}
            aria-label={interactive ? `Select tile ${index + 1}` : draggable ? `Move tile ${index + 1}; use left and right arrow keys to reorder` : undefined}
            aria-keyshortcuts={draggable ? 'ArrowLeft ArrowRight' : undefined}
          >
            <CardTile card={card} />
          </button>
        );
      })}
    </div>
  );
}
