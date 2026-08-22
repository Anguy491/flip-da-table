import { ArcadeButton, ArcadeDialog, ArcadeSelect } from '../arcade/ArcadeUI';

export function GuessModal({ open, guessForm, setGuessForm, onSubmit, onClose }) {
  return (
    <ArcadeDialog
      open={open}
      title="Decode the tile"
      eyebrow="Make a guess"
      onClose={onClose}
      actions={(
        <>
          <ArcadeButton variant="ghost" onClick={onClose}>Cancel</ArcadeButton>
          <ArcadeButton disabled={!guessForm.targetPlayerId} onClick={onSubmit}>Submit guess</ArcadeButton>
        </>
      )}
    >
      <div className="arcade-form-stack">
        <p className="arcade-copy text-sm">Target: <span className="arcade-code arcade-accent">{guessForm.targetPlayerId || 'No tile selected'}</span></p>
        <ArcadeSelect label="Color" value={guessForm.guessColor} onChange={(event) => setGuessForm((current) => ({ ...current, guessColor: event.target.value }))}>
          <option value="BLACK">Black</option>
          <option value="WHITE">White</option>
        </ArcadeSelect>
        <ArcadeSelect
          label="Number"
          value={guessForm.joker ? '_' : String(guessForm.guessValue)}
          onChange={(event) => {
            const value = event.target.value;
            setGuessForm((current) => ({ ...current, joker: value === '_', guessValue: value }));
          }}
        >
          <option value="_">Joker</option>
          {Array.from({ length: 12 }, (_, index) => <option key={index} value={index}>{index}</option>)}
        </ArcadeSelect>
      </div>
    </ArcadeDialog>
  );
}
