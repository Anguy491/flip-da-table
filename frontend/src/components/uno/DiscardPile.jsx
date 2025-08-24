import React from 'react';

export default function DiscardPile({ top }) {
  if (!top) return <div className="w-20 h-28 border-2 border-dashed rounded flex items-center justify-center text-xs opacity-60">No Top</div>;
  const { color, value } = top;
  const colorClass = {
    RED: 'bg-red-500', GREEN: 'bg-green-500', BLUE: 'bg-blue-500', YELLOW: 'bg-yellow-400 text-black'
  }[color] || 'bg-gray-600';
  const label = value === 'DRAW_TWO' ? '+2' : value === 'WILD_DRAW_FOUR' ? '+4' : value === 'REVERSE' ? '↺' : value === 'SKIP' ? '⦸' : value;
  return (
    <div className={`w-20 h-28 rounded-lg border-2 border-white shadow flex items-center justify-center font-bold text-white ${colorClass}`}>
      <span className="text-sm px-1 text-center leading-tight">{label}</span>
    </div>
  );
}
