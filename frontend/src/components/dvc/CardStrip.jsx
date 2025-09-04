import React from 'react';
import { CardTile } from './CardTile';
import { parseCard } from './parseCard';

/**
 * Draggable card strip (optional): if onReorder provided enable drag interactions.
 */
export function CardStrip({ cards, draggable=false, onReorder }) {
  const handleDragStart = (e, index) => {
    if (!draggable) return;
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', String(index));
  };
  const handleDragOver = (e) => { if (draggable) { e.preventDefault(); e.dataTransfer.dropEffect='move'; } };
  const handleDrop = (e, to) => {
    if (!draggable) return;
    e.preventDefault();
    const from = Number(e.dataTransfer.getData('text/plain'));
    if (Number.isNaN(from) || from === to) return;
    onReorder && onReorder(from, to);
  };
  return (
    <div className="dvc-card-strip flex gap-1 select-none">
      {(cards||[]).map((c,i)=> {
        const card = typeof c==='string'?parseCard(c):c;
        return (
          <div key={i}
            draggable={draggable}
            onDragStart={(e)=>handleDragStart(e,i)}
            onDragOver={handleDragOver}
            onDrop={(e)=>handleDrop(e,i)}
            className={draggable? 'transition-transform hover:-translate-y-1 cursor-grab active:cursor-grabbing': ''}
          >
            <CardTile card={card} />
          </div>
        );
      })}
    </div>
  );
}
