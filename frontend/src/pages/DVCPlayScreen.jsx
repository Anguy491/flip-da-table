import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useContext, useEffect, useState, useCallback, useMemo } from 'react';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import { AuthContext } from '../context/AuthContext';
import { fetchDvcView, drawColor, guess as apiGuess, revealDecision, selfReveal, settle } from '../api/dvc';
import { parseCard } from '../components/dvc/parseCard';
import { PlayerList } from '../components/dvc/PlayerList';
import { MyHandPanel } from '../components/dvc/MyHandPanel';
import { PendingCardBox } from '../components/dvc/PendingCardBox';
import { ControlPanel } from '../components/dvc/ControlPanel';
import { InfoPanel } from '../components/dvc/InfoPanel';
import { GuessModal } from '../components/dvc/GuessModal';

// parseCard moved to components/dvc/parseCard.js

export default function DVCPlayScreen({ initial }) {
	const { state } = useLocation();
	const { sessionid } = useParams();
	const nav = useNavigate();
	const { token } = useContext(AuthContext);

	const base = initial || state; // allow prop override
	const gameId = base?.gameId;
	const roundIndex = base?.roundIndex || 1;
	const myPlayerId = base?.myPlayerId || base?.playerId;
	const lobbyPlayers = base?.players || [];

	const [view, setView] = useState(base?.view || null);
	const [loading] = useState(!base?.view);
	const [error, setError] = useState('');
	const [loadingAction, setLoadingAction] = useState(false);
	const [showGuess, setShowGuess] = useState(false);
	const [guessForm, setGuessForm] = useState({ targetPlayerId: '', targetIndex: 0, guessColor: 'BLACK', guessValue: '0', joker: false });
	const [pendingCard, setPendingCard] = useState(null); // simple string for now

	useEffect(()=>{ if(!token) nav('/login'); },[token, nav]);

	// Polling (simple) every 2s if game running
	useEffect(()=>{
		if (!gameId || !myPlayerId) return;
		let t; let cancelled=false;
		const loop = async () => {
			try {
				const v = await fetchDvcView(gameId, myPlayerId, token);
				if (!cancelled) setView(v);
			} catch { /* ignore poll errors */ }
			if (!cancelled && !(view?.board?.winnerId)) t = setTimeout(loop, 2000);
		};
		if (!base?.view) loop();
		return () => { cancelled=true; if (t) clearTimeout(t); };
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [gameId, myPlayerId]);

	const board = view?.board;
	const playerViews = view?.players || [];
	const currentPlayerId = board && playerViews[board.currentPlayerIndex]?.playerId;
	const isMyTurn = currentPlayerId === myPlayerId;
	const awaiting = board?.awaiting;

	const meView = playerViews.find(p => p.playerId === myPlayerId);
	const myCards = useMemo(()=> (meView?.cards || []).map(parseCard), [meView]);

	const opponents = playerViews.filter(p => p.playerId !== myPlayerId);

	const disabled = !isMyTurn || !!board?.winnerId || loadingAction;

	const doDrawColor = async (color) => {
		if (disabled || awaiting !== 'DRAW_COLOR') return;
		setLoadingAction(true); setError('');
		try {
			await drawColor(gameId, myPlayerId, color, token);
			const v = await fetchDvcView(gameId, myPlayerId, token); setView(v);
			// server put pending card hidden until settle; to show we diff last card? Skip for now.
		} catch(e){ setError(e.message||'Draw failed'); } finally { setLoadingAction(false); }
	};

	const submitGuess = async () => {
		if (disabled || awaiting !== 'GUESS_SELECTION') return;
		setLoadingAction(true); setError('');
		try {
			const joker = guessForm.joker || guessForm.guessValue === '_';
			const num = joker ? null : Number(guessForm.guessValue);
			await apiGuess(gameId, myPlayerId, guessForm.targetPlayerId, Number(guessForm.targetIndex), joker, num, token);
			const v = await fetchDvcView(gameId, myPlayerId, token); setView(v);
			setShowGuess(false);
		} catch(e){ setError(e.message||'Guess failed'); } finally { setLoadingAction(false); }
	};

	const continueReveal = async (cont) => {
		if (disabled || awaiting !== 'REVEAL_DECISION') return;
		setLoadingAction(true); setError('');
		try {
			await revealDecision(gameId, myPlayerId, cont, token);
			const v = await fetchDvcView(gameId, myPlayerId, token); setView(v);
		} catch(e){ setError(e.message||'Decision failed'); } finally { setLoadingAction(false); }
	};

	const doSelfReveal = async (idx) => {
		if (disabled || awaiting !== 'SELF_REVEAL_CHOICE') return;
		setLoadingAction(true); setError('');
		try {
			await selfReveal(gameId, myPlayerId, idx, token);
			const v = await fetchDvcView(gameId, myPlayerId, token); setView(v);
		} catch(e){ setError(e.message||'Self reveal failed'); } finally { setLoadingAction(false); }
	};

	const doSettle = async (insertIndex=null) => {
		if (disabled || awaiting !== 'SETTLE_POSITION') return;
		setLoadingAction(true); setError('');
		try {
			await settle(gameId, myPlayerId, insertIndex, token);
			const v = await fetchDvcView(gameId, myPlayerId, token); setView(v); setPendingCard(null);
		} catch(e){ setError(e.message||'Settle failed'); } finally { setLoadingAction(false); }
	};

// Components extracted to /components/dvc

	if (!gameId) {
		return (
			<PageContainer>
				<CardContainer className="max-w-xl w-full text-center">
					<h2 className="text-xl font-semibold mb-4">Da Vinci Code</h2>
					<div className="text-error">No game started.</div>
					<SubmitButton type="button" className="btn-secondary mt-4" onClick={()=>nav(-1)}>Back</SubmitButton>
				</CardContainer>
			</PageContainer>
		);
	}

	return (
		<PageContainer>
			<CardContainer noMax className="dvc-screen w-auto! h-auto! max-w-6xl mx-auto flex flex-col gap-2 p-2">
				<div className="flex justify-between items-center text-xs">
					<div>Session <span className="font-mono font-semibold">{sessionid}</span></div>
					<div>Game <span className="font-mono font-semibold">{gameId}</span></div>
					<button type="button" className="btn btn-ghost btn-xs" onClick={()=>nav(-1)}>Back</button>
				</div>
				{error && <div className="alert alert-error py-1 px-2 text-xs">{error}</div>}
				{board?.winnerId && <div className="alert alert-success py-1 px-2 text-xs">Winner: {board.winnerId}</div>}
				<div className="dvc-players bg-base-200/40 rounded p-2 flex flex-col gap-2 overflow-y-auto">
					<PlayerList playerViews={playerViews} currentPlayerId={currentPlayerId} myPlayerId={myPlayerId} />
				</div>
				<div className="dvc-bottom grid grid-cols-12 gap-2 mt-2">
					<div className="col-span-6 md:col-span-6 dvc-myhand p-2 bg-base-200/40 rounded">
						<MyHandPanel cards={myCards} />
					</div>
					<div className="col-span-3 md:col-span-3 flex flex-col gap-2">
						<PendingCardBox pending={pendingCard} />
					</div>
					<div className="col-span-3 md:col-span-3 flex flex-col gap-2">
						<InfoPanel deckRemaining={board?.deckRemaining} currentPlayerId={currentPlayerId} roundIndex={roundIndex} awaiting={awaiting} />
						<ControlPanel awaiting={awaiting} disabled={disabled} myCards={myCards} doDrawColor={doDrawColor} continueReveal={continueReveal} doSelfReveal={doSelfReveal} doSettle={doSettle} openGuess={()=>setShowGuess(true)} />
					</div>
				</div>
			</CardContainer>
			<GuessModal open={showGuess} opponents={opponents} guessForm={guessForm} setGuessForm={setGuessForm} onSubmit={submitGuess} onClose={()=>setShowGuess(false)} />
		</PageContainer>
	);
}
