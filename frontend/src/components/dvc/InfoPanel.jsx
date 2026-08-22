export function InfoPanel({ deckRemaining, deckBlackRemaining = 0, deckWhiteRemaining = 0, currentPlayerId, roundIndex, awaiting }) {
  return (
    <dl className="dvc-info">
      <div className="dvc-info__row"><dt>Deck</dt><dd>{deckRemaining ?? deckBlackRemaining + deckWhiteRemaining}</dd></div>
      <div className="dvc-info__row">
        <dt>Tiles</dt>
        <dd><span className="dvc-swatch dvc-swatch--black" aria-hidden="true" /> {deckBlackRemaining} / <span className="dvc-swatch dvc-swatch--white" aria-hidden="true" /> {deckWhiteRemaining}</dd>
      </div>
      <div className="dvc-info__row"><dt>Current</dt><dd className="arcade-code max-w-32" title={currentPlayerId}>{currentPlayerId || 'Waiting'}</dd></div>
      <div className="dvc-info__row"><dt>Round</dt><dd>{roundIndex}</dd></div>
      <div className="dvc-info__row"><dt>Phase</dt><dd className="arcade-accent">{awaiting || 'Syncing'}</dd></div>
    </dl>
  );
}
