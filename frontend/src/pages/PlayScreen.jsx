import { useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import { AuthContext } from '../context/AuthContext';
import useUnoGame from '../hooks/useUnoGame';
import DiscardPile from '../components/uno/DiscardPile';
import ChooseColorModal from '../components/uno/ChooseColorModal';
import ResultOverlay from '../components/uno/ResultOverlay';
import GameOverModal from '../components/uno/GameOverModal';
import { startNextGame } from '../api/sessions';
// New layout components
import PlayerArea from '../components/uno/layout/PlayerArea';
import InfoPanel from '../components/uno/layout/InfoPanel';
import EventLog from '../components/uno/layout/EventLog';
import HandArea from '../components/uno/layout/HandArea';

/**
 * Refactored PlayScreen implementing specified flex layout.
 * Backwards compatibility: if props not provided (router usage), falls back to internal hook logic.
 * Props (presentation mode): players, currentPlayerId, direction, gameCount, activeColor, pendingDraw, lastCard, events, hand, onPlay, onDraw
 */
export default function PlayScreen(presentationalProps) {
	const { state } = useLocation();
	const { sessionid } = useParams();
	const nav = useNavigate();
	const { token } = useContext(AuthContext);

	// state from lobby navigation: { gameId, roundIndex, players, totalRounds, results }
	const gameId = state?.gameId;
	const roundIndex = state?.roundIndex ?? 1;
	const totalRounds = state?.totalRounds ?? 1;
	const playersMeta = state?.players || [];
	const pastResults = state?.results || [];
	// For MVP we don't persist playerId separately; use first non-bot? Provide via state later.
	const playerId = state?.playerId || state?.players?.find(p => !p.bot)?.playerId; // fallback (should be provided)

	useEffect(() => {
		if (!gameId) return; // fine
		if (!token) nav('/login');
	}, [token, gameId, nav]);

		// If presentationalProps has players assume external control; otherwise derive from hook
		const inPresentationalMode = !!presentationalProps?.players;

		const uno = useUnoGame({ gameId: inPresentationalMode ? undefined : gameId, playerId: inPresentationalMode ? undefined : playerId, token });
		const { view, loading, error, myTurn, hand, playableCards, mustChooseColor, pendingDraw, isFinished, sending } = inPresentationalMode ? {
			view: null,
			loading: false,
			error: null,
			myTurn: true,
			hand: presentationalProps.hand || [],
			playableCards: presentationalProps.hand || [],
			mustChooseColor: false,
			pendingDraw: presentationalProps.pendingDraw || 0,
			isFinished: false,
			sending: false
		} : uno;
		const { playCard, drawCard, chooseColor } = inPresentationalMode ? {
			playCard: (c) => presentationalProps.onPlay?.(c.id || c),
			drawCard: () => presentationalProps.onDraw?.(),
			chooseColor: () => {}
		} : uno.actions;

		const currentPlayerId = useMemo(() => inPresentationalMode ? presentationalProps.currentPlayerId : view?.players?.find(p => p.isCurrent)?.playerId, [inPresentationalMode, presentationalProps, view]);

	// winner detection (handSize === 0)
	const winner = useMemo(() => view?.players?.find(p => p.handSize === 0), [view]);
	const [modalOpen, setModalOpen] = useState(false);
	useEffect(() => { if (winner && !modalOpen) setModalOpen(true); }, [winner, modalOpen]);

	const winnerName = useMemo(() => {
		if (!winner) return '';
		const meta = playersMeta.find(pm => pm.playerId === winner.playerId);
		return meta?.name || winner.playerId;
	}, [winner, playersMeta]);

	// Persist result to sessionStorage for summary
	useEffect(() => {
		if (!winner) return;
		const key = `uno-results-${sessionid}`;
		let stored = { totalRounds, results: [], playersMeta };
		try { const raw = sessionStorage.getItem(key); if (raw) stored = JSON.parse(raw); } catch { /* ignore */ }
		// ensure we always persist latest playersMeta (names may change)
		stored.playersMeta = playersMeta;
		if (!stored.results.some(r => r.round === roundIndex)) {
			stored.totalRounds = totalRounds;
			stored.results.push({ round: roundIndex, winnerId: winner.playerId, turns: view?.turnCount || 0 });
			sessionStorage.setItem(key, JSON.stringify(stored));
		}
	}, [winner, roundIndex, sessionid, totalRounds, playersMeta, view]);

	const startNext = useCallback(async () => {
		if (roundIndex >= totalRounds) return;
		try {
			const payloadPlayers = playersMeta.map(p => ({ name: p.name, bot: p.bot, ready: true }));
			const resp = await startNextGame(sessionid, { rounds: totalRounds, players: payloadPlayers }, token);
			nav(`/playscreen/${sessionid}` , { state: { gameId: resp.gameId, roundIndex: resp.roundIndex, playerId: resp.myPlayerId, players: resp.players, totalRounds, results: [...pastResults, { round: roundIndex, winnerId: winner.playerId, winnerName, turns: view?.turnCount || 0 }] } });
		} catch (e) { console.error(e); }
	}, [roundIndex, totalRounds, playersMeta, sessionid, token, nav, pastResults, winner, winnerName, view]);

	const goSummary = useCallback(() => {
		nav(`/sessionsum/${sessionid}`);
	}, [nav, sessionid]);

	const leaveDashboard = useCallback(() => { nav('/dashboard'); }, [nav]);

	if (!gameId && !inPresentationalMode) {
		return (
			<PageContainer>
				<CardContainer className="max-w-xl w-full text-center">
					<h2 className="text-xl font-semibold mb-4">Play Screen</h2>
					<div className="text-error">No game started data found.</div>
					<SubmitButton type="button" className="btn-secondary mt-4" onClick={() => nav(-1)}>Back</SubmitButton>
				</CardContainer>
			</PageContainer>
		);
	}

	// Build presentation data either from props or from internal view
    const players = inPresentationalMode ? presentationalProps.players : (view?.players || []).map(p => ({ id: p.playerId, name: p.playerId.slice(0,6), handCount: p.handSize }));
    const direction = inPresentationalMode ? presentationalProps.direction : (view?.direction || 'CW');
    const gameCount = inPresentationalMode ? presentationalProps.gameCount : roundIndex;
    const activeColor = inPresentationalMode ? presentationalProps.activeColor : (view?.activeColor || view?.top?.color);
    const lastCard = inPresentationalMode ? presentationalProps.lastCard : view?.top;
    const events = inPresentationalMode ? presentationalProps.events : [];
    const handCards = inPresentationalMode ? presentationalProps.hand : hand;
    const playableIds = new Set(playableCards.map(c => c.id || c));

	return (
		<PageContainer>
			<div className="w-full max-w-6xl mx-auto flex flex-col gap-2">{/* main vertical container */}
				{/* Top meta bar */}
				<div className="flex justify-between items-center text-xs px-2">
					<div className="opacity-70">Session <span className="font-mono font-semibold">{sessionid}</span></div>
					<div className="opacity-70">Game <span className="font-mono font-semibold">{gameId}</span></div>
					<button type="button" className="btn btn-ghost btn-xs" onClick={() => nav(-1)}>Back</button>
				</div>
				<CardContainer noMax className="flex-1 flex flex-col p-2 h-full">{/* Outer card hosting the 1:3:2 layout (override default max-w) */}
					<div className="flex flex-col h-full w-full">
						{/* PlayerArea */}
						<div className="flex-[1_1_0] border-b mb-1 pb-1"><PlayerArea players={players} currentPlayerId={currentPlayerId} /></div>
						{/* GameMain */}
						<div className="flex-[3_1_0] flex flex-row gap-2 py-1">
							{/* DiscardPile column */}
							<div className="flex-[1_1_0] flex items-center justify-center"><DiscardPile top={lastCard} /></div>
							{/* InfoPanel column */}
							<div className="flex-[2_1_0] bg-base-200/40 rounded"><InfoPanel gameCount={gameCount} direction={direction} activeColor={activeColor} currentPlayerName={players.find(p=>p.id===currentPlayerId)?.name} pendingDraw={pendingDraw} /></div>
							{/* EventLog column */}
							<div className="flex-[3_1_0]"><EventLog events={events} /></div>
						</div>
						{/* HandArea */}
						<div className="flex-[2_1_0] mt-1 pt-1 border-t">
							<HandArea hand={handCards} playableIds={playableIds} disabled={!myTurn || mustChooseColor || isFinished} onPlay={playCard} onDraw={drawCard} pendingDraw={pendingDraw} />
							<div className="text-center text-xs opacity-70 mt-1">{myTurn ? (mustChooseColor ? 'Choose a color.' : playableCards.length ? 'Select a playable card or draw.' : 'No playable card: draw a card.') : 'Waiting for opponent / bot...'}</div>
						</div>
					</div>
				</CardContainer>
			</div>
			<ChooseColorModal open={mustChooseColor && myTurn} onPick={chooseColor} />
			<ResultOverlay open={isFinished} players={view?.players || []} onClose={() => nav(-1)} />
			<GameOverModal
				open={modalOpen && !!winner}
				winnerName={winnerName}
				winnerId={winner?.playerId}
				turns={view?.turnCount || 0}
				onClose={leaveDashboard}
				onNext={startNext}
				isLast={roundIndex >= totalRounds}
				onSummary={goSummary}
			/>
		</PageContainer>
	);
}
