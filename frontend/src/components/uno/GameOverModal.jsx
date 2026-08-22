import { ArcadeButton, ArcadeDialog } from '../arcade/ArcadeUI';

export default function GameOverModal({ open, winnerName, winnerId, turns, onClose, onNext, isLast, onSummary }) {
  return (
    <ArcadeDialog
      open={open}
      title="Game over"
      eyebrow="High score recorded"
      dismissible={false}
      actions={(
        <>
          <ArcadeButton variant="ghost" onClick={onClose}>Dashboard</ArcadeButton>
          {isLast
            ? <ArcadeButton onClick={onSummary}>View summary</ArcadeButton>
            : <ArcadeButton onClick={onNext}>Next game</ArcadeButton>}
        </>
      )}
    >
      <div className="text-center py-5">
        <p className="arcade-eyebrow">Winner</p>
        <p className="arcade-title break-words">{winnerName || winnerId}</p>
        <p className="arcade-copy mt-4">Match completed in <strong className="arcade-accent">{turns}</strong> turns.</p>
      </div>
    </ArcadeDialog>
  );
}
