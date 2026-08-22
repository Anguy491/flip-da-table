import { ArcadeDialog } from '../arcade/ArcadeUI';

const colors = ['RED', 'GREEN', 'BLUE', 'YELLOW'];

export default function ChooseColorModal({ open, onPick, onHide, disabled }) {
  return (
    <ArcadeDialog open={open} title="Pick the next color" eyebrow="Wild card" closeLabel="Hide picker" onClose={onHide}>
      <div className="uno-color-grid">
        {colors.map((color) => (
          <button key={color} type="button" disabled={disabled} onClick={() => onPick(color)} className="uno-color-choice" data-color={color}>{color}</button>
        ))}
      </div>
      <p className="arcade-copy text-sm mt-5">Choose a color to complete your turn, or hide this picker to review your hand.</p>
    </ArcadeDialog>
  );
}
