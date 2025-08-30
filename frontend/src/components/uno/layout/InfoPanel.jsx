import React from 'react';

export default function InfoPanel({ gameCount, direction, activeColor, currentPlayerName, pendingDraw }) {
  const dirText = direction === 'CCW' ? 'Counter Clockwise' : 'Clockwise';
  const colorMap = {
    RED: 'bg-red-500',
    GREEN: 'bg-green-500',
    BLUE: 'bg-blue-500',
    YELLOW: 'bg-yellow-400 text-black'
  };
  return (
    <div className="w-full h-full flex flex-col gap-2 text-xs p-2">
      <div className="grid grid-cols-2 gap-y-1 gap-x-2">
        <span className="font-semibold">Round</span><span>{gameCount}</span>
        <span className="font-semibold">Direction</span><span>{dirText}</span>
        <span className="font-semibold">Active Color</span>
        <span className="flex items-center gap-1">
          <span className={`w-4 h-4 rounded ${colorMap[activeColor] || 'bg-gray-400'}`}></span>
          <span>{activeColor || '—'}</span>
        </span>
        <span className="font-semibold">Current Player</span><span className="font-medium text-primary truncate" title={currentPlayerName}>{currentPlayerName || '—'}</span>
        <span className="font-semibold">Pending Draw</span><span>{pendingDraw}</span>
      </div>
    </div>
  );
}
