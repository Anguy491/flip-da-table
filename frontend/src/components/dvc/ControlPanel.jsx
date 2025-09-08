import React from 'react';

export function ControlPanel({ awaiting, disabled, myCards, doDrawColor, continueReveal, doSelfReveal, doSettle, openGuess, guessSucceeded, canSettle, settledSubmitted, isStartPhaseSettle=false, hasPending=false, isMyTurn=false }) {
  return (
    <div className="dvc-controls flex flex-col gap-2 text-xs">
      {awaiting==='SETTLE_POSITION' && (
        isStartPhaseSettle ? (
          settledSubmitted ? (
            <div className="italic opacity-80">Waiting for other players to settle...</div>
          ) : (
            <button className="btn btn-sm btn-primary" disabled={disabled || !canSettle} onClick={()=>doSettle(null)} data-testid="settle-finish">Settle</button>
          )
        ) : (
          hasPending ? (
            <button className="btn btn-sm btn-primary" disabled={disabled} onClick={()=>doSettle(null)} data-testid="settle-runtime">Settle</button>
          ) : (
            <div className="italic opacity-80">Waiting for current player to settle...</div>
          )
        )
      )}
      {awaiting==='DRAW_COLOR' && (
        <div className="flex gap-2">
          <button className="btn btn-sm btn-primary" disabled={disabled} onClick={()=>doDrawColor('BLACK')} data-testid="draw-black">Draw Black</button>
          <button className="btn btn-sm btn-secondary" disabled={disabled} onClick={()=>doDrawColor('WHITE')} data-testid="draw-white">Draw White</button>
        </div>
      )}
      {awaiting==='GUESS_SELECTION' && (
        isMyTurn
          ? <div className="italic opacity-80" data-testid="guess-instruction">select a opponent card to guess</div>
          : <div className="italic opacity-80" data-testid="opponent-turn">opponent turn</div>
      )}
      {awaiting==='REVEAL_DECISION' && guessSucceeded && (
        <div className="flex gap-2">
          <button className="btn btn-sm btn-success" disabled={disabled} onClick={()=>continueReveal(true)} data-testid="reveal-continue">Continue</button>
          <button className="btn btn-sm" disabled={disabled} onClick={()=>continueReveal(false)} data-testid="reveal-stop">Cease</button>
        </div>
      )}
      {awaiting==='SELF_REVEAL_CHOICE' && (
        <div className="flex flex-col gap-1">
          <div>Select one of your hidden cards:</div>
          <div className="flex gap-1 flex-wrap">
            {myCards.map((c,i)=>!c.revealed && <button key={i} className="btn btn-xs" onClick={()=>doSelfReveal(i)} disabled={disabled} data-testid={`self-reveal-${i}`}>{i+1}</button>)}
          </div>
        </div>
      )}
    </div>
  );
}
