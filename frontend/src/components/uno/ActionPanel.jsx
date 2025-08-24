import React from 'react';
import SubmitButton from '../SubmitButton';

export default function ActionPanel({ myTurn, canDraw, onDraw, onDeclareUno, canDeclareUno, pendingDraw, isFinished, sending }) {
  return (
    <div className="flex flex-wrap gap-3 justify-center items-center mt-4">
      <SubmitButton type="button" className="btn-secondary" disabled={!myTurn || !canDraw || isFinished || sending} onClick={onDraw}>{pendingDraw>0 ? `Draw ${pendingDraw}` : 'Draw'}</SubmitButton>
      <SubmitButton type="button" className="btn-outline" disabled={!canDeclareUno || isFinished || sending} onClick={onDeclareUno}>UNO!</SubmitButton>
    </div>
  );
}
