import { ArcadeButton, ArcadeDialog } from '../arcade/ArcadeUI';

export default function DvcGameOverModal({ open, winnerName, winnerId, turns, onClose, onSummary }) {
  return (
    <ArcadeDialog
      open={open}
      title="Code cracked"
      eyebrow="Game over"
      dismissible={false}
      actions={(
        <>
          <ArcadeButton variant="ghost" onClick={onClose}>Return to dashboard</ArcadeButton>
          <ArcadeButton onClick={onSummary}>View summary</ArcadeButton>
        </>
      )}
    >
      <div className="text-center py-5">
        <p className="arcade-eyebrow">Winner</p>
        <p className="arcade-title break-words">{winnerName || winnerId}</p>
        <p className="arcade-copy mt-4">The final code was solved in <strong className="arcade-accent">{turns}</strong> turns.</p>
      </div>
    </ArcadeDialog>
  );
}
