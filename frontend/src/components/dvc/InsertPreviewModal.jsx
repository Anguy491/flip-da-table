import { Fragment, useEffect, useMemo, useState } from 'react';
import { ArcadeButton, ArcadeDialog, StatusBanner } from '../arcade/ArcadeUI';
import { CardTile } from './CardTile';
import { parseCard } from './parseCard';
import { isArrangementValid } from './arrangement';

export function InsertPreviewModal({ open, onClose, myCards, pending, onConfirm }) {
  const baseCards = useMemo(() => (myCards || []).map((card) => typeof card === 'string' ? parseCard(card) : card), [myCards]);
  const pendingCard = useMemo(() => typeof pending === 'string' ? parseCard(pending) : pending, [pending]);
  const [insertIndex, setInsertIndex] = useState(null);

  useEffect(() => {
    if (open) setInsertIndex(null);
  }, [open, pending]);

  const previewCards = useMemo(() => {
    if (insertIndex == null || !pendingCard) return baseCards;
    const next = [...baseCards];
    next.splice(insertIndex, 0, pendingCard);
    return next;
  }, [baseCards, insertIndex, pendingCard]);

  const valid = insertIndex != null && Boolean(pendingCard) && isArrangementValid(previewCards);
  const handString = useMemo(() => {
    if (!valid) return '';
    return previewCards.map((card) => {
      const prefix = card.color === 'BLACK' ? 'B' : 'W';
      const value = card.isJoker || card.value === '-' ? '_' : card.value;
      return `${prefix}${value}≤`;
    }).join('');
  }, [previewCards, valid]);

  return (
    <ArcadeDialog
      open={open}
      title="Place the pending tile"
      eyebrow="Settle position"
      onClose={onClose}
      wide
      actions={(
        <>
          <ArcadeButton variant="ghost" onClick={onClose}>Cancel</ArcadeButton>
          <ArcadeButton disabled={!valid} onClick={() => onConfirm(handString)}>Lock position</ArcadeButton>
        </>
      )}
    >
      <p className="arcade-copy text-sm mb-4">Choose a gap. The final rack must remain in ascending order, with black before white on ties.</p>
      <div className="dvc-insert-board">
        <InsertSlot index={0} active={insertIndex === 0} valid={valid} onSelect={setInsertIndex} />
        {baseCards.map((card, index) => (
          <Fragment key={`${card.color}-${card.value}-${index}`}>
            <CardTile card={card} />
            <InsertSlot index={index + 1} active={insertIndex === index + 1} valid={valid} onSelect={setInsertIndex} />
          </Fragment>
        ))}
      </div>
      <div className="mt-5 flex items-center gap-4">
        <div>
          <p className="arcade-game-zone__title">Pending</p>
          {pendingCard && <CardTile card={pendingCard} />}
        </div>
        {insertIndex == null
          ? <StatusBanner>Select a gap to preview the final rack.</StatusBanner>
          : <StatusBanner tone={valid ? 'success' : 'error'}>{valid ? 'This position is legal.' : 'This position breaks the code order.'}</StatusBanner>}
      </div>
    </ArcadeDialog>
  );
}

function InsertSlot({ index, active, valid, onSelect }) {
  return (
    <button
      type="button"
      className={`dvc-insert-slot ${active ? valid ? 'dvc-insert-slot--valid' : 'dvc-insert-slot--active' : ''}`}
      onClick={() => onSelect(index)}
      aria-label={`Insert at position ${index + 1}`}
      aria-pressed={active}
    >
      {active ? valid ? 'OK' : 'X' : '+'}
    </button>
  );
}
