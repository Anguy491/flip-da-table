import React from 'react';

export function GuessModal({ open, opponents, guessForm, setGuessForm, onSubmit, onClose }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-base-100 rounded p-4 w-full max-w-sm flex flex-col gap-3 text-xs">
        <h3 className="font-semibold">Make a Guess</h3>
        <label className="flex flex-col gap-1">Target Player
          <select className="select select-bordered select-xs" value={guessForm.targetPlayerId} onChange={e=>setGuessForm(f=>({...f,targetPlayerId:e.target.value}))}>
            <option value="">--</option>
            {opponents.map(o=> <option key={o.playerId} value={o.playerId}>{o.playerId}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1">Card Index
          <input type="number" className="input input-bordered input-xs" value={guessForm.targetIndex} min={0} onChange={e=>setGuessForm(f=>({...f,targetIndex:e.target.value}))} />
        </label>
        <div className="flex gap-2 items-center">
          <label className="flex items-center gap-1 text-[10px]">
            <input type="checkbox" className="checkbox checkbox-xs" checked={guessForm.joker} onChange={e=>setGuessForm(f=>({...f,joker:e.target.checked, guessValue:e.target.checked?'_':'0'}))} /> Joker
          </label>
          {!guessForm.joker && (
            <select className="select select-bordered select-xs" value={guessForm.guessValue} onChange={e=>setGuessForm(f=>({...f,guessValue:e.target.value}))}>{Array.from({length:12},(_,i)=> <option key={i} value={i}>{i}</option>)}</select>
          )}
          <select className="select select-bordered select-xs" value={guessForm.guessColor} onChange={e=>setGuessForm(f=>({...f,guessColor:e.target.value}))}>
            <option value="BLACK">BLACK</option>
            <option value="WHITE">WHITE</option>
          </select>
        </div>
        <div className="flex justify-end gap-2 mt-2">
          <button className="btn btn-ghost btn-xs" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary btn-xs" disabled={!guessForm.targetPlayerId} onClick={onSubmit}>Submit</button>
        </div>
      </div>
    </div>
  );
}
