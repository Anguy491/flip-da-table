import React from 'react';
import { CardTile } from './CardTile';
import { parseCard } from './parseCard';

export function CardStrip({ cards }) {
  return <div className="dvc-card-strip flex gap-1">{(cards||[]).map((c,i)=> <CardTile key={i} card={typeof c==='string'?parseCard(c):c} />)}</div>;
}
