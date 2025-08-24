import React from 'react';

const colors = ['RED', 'GREEN', 'BLUE', 'YELLOW'];
const colorClass = {
  RED: 'bg-red-500',
  GREEN: 'bg-green-500',
  BLUE: 'bg-blue-500',
  YELLOW: 'bg-yellow-400 text-black'
};

export default function ColorPickerModal({ open, onPick, disabled }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-base-100 p-6 rounded shadow-lg space-y-4 w-64">
        <h3 className="font-semibold text-center">Choose Color</h3>
        <div className="grid grid-cols-2 gap-3">
          {colors.map(c => (
            <button key={c} disabled={disabled} onClick={() => onPick(c)} className={`h-16 rounded flex items-center justify-center text-sm font-bold border-2 ${colorClass[c]} border-white shadow`}>{c}</button>
          ))}
        </div>
      </div>
    </div>
  );
}
