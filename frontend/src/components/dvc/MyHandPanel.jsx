import React from 'react';
import { CardStrip } from './CardStrip';

// validity rule: all revealed numbers (non-hidden) must be non-decreasing left->right (simple example placeholder)
export function isArrangementValid(cards) {
  // Accept all during initial unless player wants rule: numbers ascending ignoring jokers; implement basic.
  let last = -1;
  for (const c of cards) {
    if (!c.revealed && c.value === null) continue;
    if (c.isJoker) continue;
    const v = typeof c.value === 'number'? c.value : parseInt(c.value,10);
    if (!Number.isNaN(v) && v < last) return false;
    if (!Number.isNaN(v)) last = v;
  }
  return true;
}

export function MyHandPanel({ cards, draggable, onReorder, showValidity=false }) {
  const valid = isArrangementValid(cards);
  return (
    <div className="dvc-myhand">
      <div className="flex items-center justify-between mb-1">
        <h3 className="text-xs font-semibold">My Hand</h3>
        {showValidity && <span className={`text-[10px] ${valid?'text-success':'text-error'}`}>{valid?'OK':'Invalid'}</span>}
      </div>
      <CardStrip cards={cards} draggable={draggable} onReorder={onReorder} />
    </div>
  );
}
