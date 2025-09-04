import React from 'react';

export function ControlPanel({ awaiting, disabled, myCards, doDrawColor, continueReveal, doSelfReveal, doSettle, openGuess }) {
  return (
    <div className="dvc-controls flex flex-col gap-2 text-xs">
      {awaiting==='DRAW_COLOR' && (
        <div className="flex gap-2">
          <button className="btn btn-sm btn-primary" disabled={disabled} onClick={()=>doDrawColor('BLACK')} data-testid="draw-black">Draw Black</button>
          <button className="btn btn-sm btn-secondary" disabled={disabled} onClick={()=>doDrawColor('WHITE')} data-testid="draw-white">Draw White</button>
        </div>
      )}
      {awaiting==='GUESS_SELECTION' && (
        <button className="btn btn-sm btn-accent" disabled={disabled} onClick={openGuess} data-testid="open-guess">Guess</button>
      )}
      {awaiting==='REVEAL_DECISION' && (
        <div className="flex gap-2">
          <button className="btn btn-sm btn-success" disabled={disabled} onClick={()=>continueReveal(true)} data-testid="reveal-continue">Continue</button>
          <button className="btn btn-sm" disabled={disabled} onClick={()=>continueReveal(false)} data-testid="reveal-stop">Stop</button>
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
      {awaiting==='SETTLE_POSITION' && (
        <div className="flex flex-col gap-1">
          <div>Insert position:</div>
          <div className="flex gap-1 flex-wrap">
            {Array.from({length: myCards.length+1},(_,i)=> <button key={i} className="btn btn-xs" onClick={()=>doSettle(i)} disabled={disabled} data-testid={`settle-${i}`}>{i}</button>)}
          </div>
        </div>
      )}
    </div>
  );
}
