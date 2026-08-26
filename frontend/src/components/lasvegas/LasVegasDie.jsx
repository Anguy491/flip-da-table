export default function LasVegasDie({
  face,
  big = false,
  seatIndex = 0,
  count,
  label,
  showOwner = true,
  className = '',
  ...props
}) {
  const visibleFace = face == null ? '?' : face;

  return (
    <span
      className={`vegas-die ${big ? 'vegas-die--big' : ''} ${className}`.trim()}
      data-seat={seatIndex + 1}
      role="img"
      aria-label={label || (face == null ? 'Rolling die' : `${big ? 'Big die' : 'Die'} showing ${face}`)}
      {...props}
    >
      <span className="vegas-die__face" aria-hidden="true">{visibleFace}</span>
      {showOwner && (
        <span className="vegas-die__owner" aria-hidden="true">
          P{seatIndex + 1}{big ? ' ×2' : count ? ` ×${count}` : ''}
        </span>
      )}
    </span>
  );
}
