import React from 'react';

const colorToBg = {
  RED: 'bg-red-500',
  GREEN: 'bg-green-500',
  BLUE: 'bg-blue-500',
  YELLOW: 'bg-yellow-400 text-black'
};

export default function UnoCard({ card, onClick, disabled }) {
  const { color, value } = card;
  const isWild = value === 'WILD' || value === 'WILD_DRAW_FOUR';
  const bg = isWild ? 'bg-gradient-to-br from-gray-700 via-black to-gray-800 border-yellow-400' : colorToBg[color] || 'bg-gray-500';
  return (
    <button
      type="button"
      onClick={() => !disabled && onClick?.(card)}
      className={`relative w-16 h-24 rounded-lg border-2 text-white font-bold flex items-center justify-center shadow-md transition-transform ${bg} ${disabled ? 'opacity-40 cursor-not-allowed' : 'hover:scale-105 active:scale-95'}`}
      aria-disabled={disabled}
    >
      <span className="text-sm text-center px-1 leading-tight">
        {value === 'DRAW_TWO' ? '+2' : value === 'WILD_DRAW_FOUR' ? '+4' : value === 'REVERSE' ? '↺' : value === 'SKIP' ? '⦸' : value}
      </span>
    </button>
  );
}
