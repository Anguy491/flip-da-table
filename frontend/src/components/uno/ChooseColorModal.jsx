import React from 'react';

const colors = ['RED', 'GREEN', 'BLUE', 'YELLOW'];
const colorClass = {
  RED: 'bg-red-500',
  GREEN: 'bg-green-500',
  BLUE: 'bg-blue-500',
  YELLOW: 'bg-yellow-400 text-black'
};

export default function ChooseColorModal({ open, onPick, disabled }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-base-100 p-6 rounded shadow-lg space-y-4 w-64">
        <h3 className="font-semibold text-center">Pick a Color</h3>
        <div className="grid grid-cols-2 gap-3">
          {colors.map(c => (
            <button key={c} disabled={disabled} onClick={() => onPick(c)} className={`h-16 rounded flex items-center justify-center text-sm font-bold border-2 ${colorClass[c]} border-white shadow active:scale-95 transition`}>{c}</button>
          ))}
        </div>
        <div className="text-center text-xs opacity-70">Click a color to complete Wild color selection</div>
      </div>
    </div>
  );
}
