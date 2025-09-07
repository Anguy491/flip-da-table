import React, { useMemo, useState } from 'react';
import { CardTile } from './CardTile';
import { parseCard } from './parseCard';
import { isArrangementValid } from './MyHandPanel';

/**
 * InsertPreviewModal: shows a non-interactive clone of the player's hand and a draggable pending card.
 * Only the pending card can be moved among gap slots (0..n). Confirm enabled only when the resulting
 * sequence is valid per isArrangementValid.
 */
export function InsertPreviewModal({ open, onClose, myCards, pending, onConfirm }) {
  if (!open) return null;
  const baseCards = useMemo(() => (myCards || []).map(c => (typeof c==='string' ? parseCard(c) : c)), [myCards]);
  const pendingCard = useMemo(() => (typeof pending==='string' ? parseCard(pending) : pending), [pending]);

  // No slot selected by default; user must click a gap.
  const [insertIndex, setInsertIndex] = useState(null);

  const previewCards = useMemo(() => {
    if (insertIndex === null || insertIndex === undefined) return baseCards;
    const arr = [...baseCards];
    const before = arr.slice(0, insertIndex);
    const after = arr.slice(insertIndex);
    return [...before, pendingCard, ...after];
  }, [baseCards, pendingCard, insertIndex]);

  const valid = useMemo(() => {
    if (insertIndex === null || insertIndex === undefined) return false;
    return isArrangementValid(previewCards);
  }, [previewCards, insertIndex]);

  const handString = useMemo(() => {
    if (!valid) return '';
    // Build token string for entire new hand
    const tokens = (previewCards || []).map(c => {
      const prefix = c.color === 'BLACK' ? 'B' : 'W';
      const val = c.isJoker || c.value === '-' ? '_' : c.value;
      return prefix + String(val) + '≤';
    });
    return tokens.join('');
  }, [previewCards, valid]);

  const onSelectGap = (idx) => setInsertIndex(idx);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-base-100 rounded p-4 w-full max-w-2xl flex flex-col gap-3 text-xs">
  <h3 className="font-semibold">Insert Pending Card</h3>
  <div className="text-[11px] opacity-80">Click a gap between cards to choose where to insert the pending card.</div>
        <div className="relative border rounded p-3 bg-base-200/30">
          <div className="flex items-stretch gap-1">
            {/* Left slot */}
            <ClickSlot index={0} active={insertIndex===0} activeValid={valid} onSelect={onSelectGap} />
            {baseCards.map((c, i) => (
              <React.Fragment key={i}>
                <CardTile card={c} />
                <ClickSlot index={i+1} active={insertIndex===i+1} activeValid={valid} onSelect={onSelectGap} />
              </React.Fragment>
            ))}
          </div>
          {/* Pending card preview */}
          <div className="mt-3 flex items-center gap-2">
            <div className="font-semibold">Pending:</div>
            <div>
              <CardTile card={pendingCard} />
            </div>
          </div>
        </div>
        <div className="flex items-center justify-between">
          <div className={`text-[10px] ${valid ? 'text-success' : 'text-error'}`}>
            {insertIndex === null ? 'Please select a position' : (valid ? 'Position OK' : 'Illegal position')}
          </div>
          <div className="flex gap-2">
            <button className="btn btn-ghost btn-xs" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary btn-xs" disabled={!valid} onClick={()=>onConfirm(handString)}>Confirm</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function ClickSlot({ index, active, activeValid, onSelect }) {
  const handleClick = () => onSelect(index);
  const baseClasses = 'relative w-6 min-h-24 rounded-md border-dashed border-2 flex-shrink-0 transition-colors cursor-pointer';
  const stateClasses = active
    ? (activeValid ? 'border-success bg-success/10' : 'border-error bg-error/10')
    : 'border-base-300/60 hover:border-primary/60';

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={handleClick}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onSelect(index); }}
      className={[baseClasses, stateClasses].join(' ')}
      aria-label={`Insert at ${index}`}
    >
      {active && (
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className={[
            'w-5 h-5 rounded-full border-2 flex items-center justify-center text-[10px] font-bold bg-base-100/70 backdrop-blur',
            activeValid ? 'border-success text-success' : 'border-error text-error',
          ].join(' ')}>
            {activeValid ? '✓' : '✕'}
          </div>
        </div>
      )}
    </div>
  );
}
