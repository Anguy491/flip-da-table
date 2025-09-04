import React from 'react';
import { CardStrip } from './CardStrip';

export function MyHandPanel({ cards }) {
  return (
    <div className="dvc-myhand">
      <h3 className="text-xs font-semibold mb-1">My Hand</h3>
      <CardStrip cards={cards} />
    </div>
  );
}
