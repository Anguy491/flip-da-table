import { parseCard } from './parseCard';

export function CardTile({ card: raw, className = '' }) {
  const card = typeof raw === 'string' ? parseCard(raw) : raw;
  const color = card?.color || 'BLACK';
  const value = card?.revealed ? (card.isJoker ? '—' : card.value) : '?';
  const label = card?.isJoker ? 'Joker tile' : card?.revealed ? `${color} tile ${card.value}` : `Hidden ${color} tile`;
  return (
    <div className={`dvc-card ${className}`} data-color={color} data-hidden={card?.revealed ? 'false' : 'true'} aria-label={label}>
      <span>{value}</span>
    </div>
  );
}
