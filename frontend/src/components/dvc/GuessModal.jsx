import React from 'react';

// Redesigned to only include two selections: Color (BLACK/WHITE) and Number (0-11 or Joker)
export function GuessModal({ open, opponents, guessForm, setGuessForm, onSubmit, onClose }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-base-100 rounded p-4 w-full max-w-sm flex flex-col gap-3 text-xs">
        <h3 className="font-semibold">Make a Guess</h3>
        <label className="flex flex-col gap-1">Color
          <select
            className="select select-bordered select-xs"
            value={guessForm.guessColor}
            onChange={e=>setGuessForm(f=>({...f, guessColor: e.target.value}))}
          >
            <option value="BLACK">BLACK</option>
            <option value="WHITE">WHITE</option>
          </select>
        </label>
        <label className="flex flex-col gap-1">Number
          <select
            className="select select-bordered select-xs"
            value={guessForm.joker ? '_' : String(guessForm.guessValue)}
            onChange={e=>{
              const v = e.target.value;
              if (v === '_') setGuessForm(f=>({...f, joker:true, guessValue:'_'}));
              else setGuessForm(f=>({...f, joker:false, guessValue:v}));
            }}
          >
            <option value="_">JOKER</option>
            {Array.from({length:12},(_,i)=> <option key={i} value={i}>{i}</option>)}
          </select>
        </label>
        <div className="flex justify-end gap-2 mt-2">
          <button className="btn btn-ghost btn-xs" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary btn-xs" disabled={!guessForm.targetPlayerId} onClick={onSubmit}>Submit</button>
        </div>
      </div>
    </div>
  );
}
