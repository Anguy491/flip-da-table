export default function InfoPanel({ gameCount, direction, activeColor, currentPlayerName, pendingDraw }) {
  const directionText = direction === 'CCW' ? 'Counter-clockwise' : 'Clockwise';
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-xs">
      <dt className="arcade-muted">Round</dt><dd className="font-bold">{gameCount}</dd>
      <dt className="arcade-muted">Direction</dt><dd>{directionText}</dd>
      <dt className="arcade-muted">Active color</dt>
      <dd className="flex items-center gap-2"><span className="uno-color-chip" data-color={activeColor || 'NONE'} aria-hidden="true" /><span>{activeColor || 'None'}</span></dd>
      <dt className="arcade-muted">Current player</dt><dd className="arcade-accent truncate" title={currentPlayerName}>{currentPlayerName || 'Waiting'}</dd>
      <dt className="arcade-muted">Draw stack</dt><dd className={pendingDraw ? 'arcade-error-text font-bold' : ''}>{pendingDraw || 0}</dd>
    </dl>
  );
}
