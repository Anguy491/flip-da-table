const valueLabels = {
  DRAW_TWO: '+2',
  WILD_DRAW_FOUR: '+4',
  REVERSE: 'REV',
  SKIP: 'SKIP',
  WILD: 'WILD',
};

export default function UnoCard({ card, onClick, disabled = false, discard = false }) {
  const color = card?.color || 'NONE';
  const value = card?.value || '?';
  const wild = value === 'WILD' || value === 'WILD_DRAW_FOUR';
  const label = valueLabels[value] || value;

  if (discard) {
    return (
      <div className="uno-card uno-card--discard" data-color={color} data-wild={wild ? 'true' : 'false'} aria-label={`Top card ${color} ${label}`}>
        <span className="uno-card__value">{label}</span>
      </div>
    );
  }

  return (
    <button
      type="button"
      className="uno-card"
      data-color={color}
      data-wild={wild ? 'true' : 'false'}
      disabled={disabled}
      onClick={() => onClick?.(card)}
      aria-label={`${color} ${label}${disabled ? ', not playable' : ', play card'}`}
    >
      <span className="uno-card__value">{label}</span>
    </button>
  );
}
