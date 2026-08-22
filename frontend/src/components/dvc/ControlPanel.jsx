import { ArcadeButton, StatusBanner } from '../arcade/ArcadeUI';

const phaseCopy = {
  SETTLE_POSITION: 'Arrange your tiles from low to high. Black comes before white when values match.',
  DRAW_COLOR: 'Choose which color pile to draw from.',
  GUESS_SELECTION: 'Select one hidden opponent tile to make a guess.',
  REVEAL_DECISION: 'Correct guess. Continue your run or stop safely.',
  SELF_REVEAL_CHOICE: 'Choose one private tile to reveal after an incorrect guess.',
};

export function ControlPanel({ awaiting, disabled, doDrawColor, continueReveal, doSelfReveal, doSettle, guessSucceeded, canSettle, settledSubmitted, isStartPhaseSettle = false, hasPending = false, isMyTurn = false, selfRevealIndex = null, blackRemaining = 0, whiteRemaining = 0 }) {
  const notMyTurn = !isMyTurn && awaiting !== 'SETTLE_POSITION';
  if (notMyTurn) return <StatusBanner>Opponent turn. Watch the racks for new information.</StatusBanner>;

  return (
    <div className="dvc-controls">
      <div className="dvc-phase" data-testid="phase-instruction">{phaseCopy[awaiting] || 'Waiting for the next phase...'}</div>
      {awaiting === 'SETTLE_POSITION' && (
        isStartPhaseSettle
          ? settledSubmitted
            ? <StatusBanner tone="success">Your rack is locked. Waiting for other players.</StatusBanner>
            : <ArcadeButton disabled={disabled || !canSettle} onClick={() => doSettle(null)} data-testid="settle-finish">Lock rack</ArcadeButton>
          : hasPending
            ? <ArcadeButton disabled={disabled} onClick={() => doSettle(null)} data-testid="settle-runtime">Place pending tile</ArcadeButton>
            : <StatusBanner>Waiting for the active player.</StatusBanner>
      )}
      {awaiting === 'DRAW_COLOR' && (
        <div className="arcade-actions">
          <ArcadeButton disabled={disabled || blackRemaining <= 0} onClick={() => doDrawColor('BLACK')} data-testid="draw-black">Draw black ({blackRemaining})</ArcadeButton>
          <ArcadeButton variant="secondary" disabled={disabled || whiteRemaining <= 0} onClick={() => doDrawColor('WHITE')} data-testid="draw-white">Draw white ({whiteRemaining})</ArcadeButton>
        </div>
      )}
      {awaiting === 'GUESS_SELECTION' && <p className="arcade-copy text-sm" data-testid="guess-instruction">Pick a hidden tile from an opponent rack above.</p>}
      {awaiting === 'REVEAL_DECISION' && guessSucceeded && (
        <div className="arcade-actions">
          <ArcadeButton variant="success" disabled={disabled} onClick={() => continueReveal(true)} data-testid="reveal-continue">Continue</ArcadeButton>
          <ArcadeButton variant="ghost" disabled={disabled} onClick={() => continueReveal(false)} data-testid="reveal-stop">Stop safely</ArcadeButton>
        </div>
      )}
      {awaiting === 'SELF_REVEAL_CHOICE' && (
        <div className="arcade-form-stack">
          <p className="arcade-copy text-sm">{selfRevealIndex == null ? 'Select one private tile in your rack.' : `Tile ${selfRevealIndex + 1} selected.`}</p>
          <ArcadeButton disabled={disabled || selfRevealIndex == null} onClick={doSelfReveal} data-testid="self-reveal-confirm">Reveal selected tile</ArcadeButton>
        </div>
      )}
    </div>
  );
}
