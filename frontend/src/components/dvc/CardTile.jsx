import React from 'react';
import { parseCard } from './parseCard';

/*
 * CardTile visual design:
 *  - Fixed aspect mini-card (width auto via content, consistent height)
 *  - Black cards: dark background, light text
 *  - White cards: light background, dark text with subtle outline
 *  - Hidden state: shows ≤ symbol, muted tone
 *  - Joker: underscore glyph, accent border
 */
export function CardTile({ card: raw, className='' }) {
  const card = typeof raw === 'string' ? parseCard(raw) : raw;
  const isBlack = card.color === 'BLACK';
  const valueGlyph = card.revealed ? (card.isJoker ? '–' : card.value) : '≤'; // use en dash for joker for visual weight
  const aria = card.isJoker ? 'Joker card' : (card.revealed ? `Card ${card.color} ${card.value}` : `Hidden ${card.color}`);

  const baseColorClasses = isBlack
    ? 'bg-neutral-900 text-neutral-100 shadow-[0_0_0_1px_rgba(255,255,255,0.15)]'
    : 'bg-neutral-50 text-neutral-900 shadow-[0_0_0_1px_rgba(0,0,0,0.15)]';
  const hiddenClasses = card.revealed ? '' : (isBlack ? 'opacity-80' : 'text-neutral-500');
  const jokerClasses = card.isJoker ? 'ring-2 ring-accent ring-offset-[1px] ring-offset-base-100' : '';

  return (
    <div
      className={[
        'dvc-card relative select-none font-mono rounded-sm flex items-center justify-center h-24 w-18 text-3xl font-semibold tracking-tight',
        'border border-neutral-700/40 dark:border-neutral-300/20',
        'transition-all duration-200',
        baseColorClasses,
        hiddenClasses,
        jokerClasses,
        className
      ].filter(Boolean).join(' ')}
      data-color={card.color}
      aria-label={aria}
    >
      <span className={card.revealed ? '' : 'text-3xl'}>{valueGlyph}</span>
      {!card.revealed && (
        <span className="pointer-events-none absolute inset-0 rounded-sm bg-gradient-to-br from-transparent to-black/5 mix-blend-overlay" />
      )}
    </div>
  );
}
