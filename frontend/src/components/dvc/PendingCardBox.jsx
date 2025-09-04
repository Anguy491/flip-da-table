import React from 'react';
import { CardTile } from './CardTile';
import { parseCard } from './parseCard';

export function PendingCardBox({ pending }) {
  return (
    <div className="dvc-pending p-2 border rounded max-w-[100px] min-h-[150px] flex flex-col items-center justify-center text-xs">
      <div className="font-semibold mb-1">Pending</div>
      {pending ? <CardTile card={parseCard(pending)} /> : <div className="opacity-40">None</div>}
    </div>
  );
}
