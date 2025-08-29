import { useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import { AuthContext } from '../context/AuthContext';
import useUnoGame from '../hooks/useUnoGame';
import UnoPlayerStrip from '../components/uno/UnoPlayerStrip';
import UnoHand from '../components/uno/UnoHand';
import DiscardPile from '../components/uno/DiscardPile';
import ChooseColorModal from '../components/uno/ChooseColorModal';
import ActionPanel from '../components/uno/ActionPanel';
import ResultOverlay from '../components/uno/ResultOverlay';
import GameOverModal from '../components/uno/GameOverModal';
import { startFirstGame, startNextGame } from '../api/sessions';

export default function PlayScreen() {
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

	const uno = useUnoGame({ gameId, playerId, token });
	const { view, loading, error, myTurn, hand, playableCards, mustChooseColor, pendingDraw, isFinished, sending } = uno;
	const { playCard, drawCard, chooseColor, declareUno } = uno.actions;

	const currentPlayerId = useMemo(() => view?.players?.find(p => p.isCurrent)?.playerId, [view]);

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

	if (!gameId) {
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

	return (
		<PageContainer>
			<div className="w-full flex flex-col gap-4 items-center">
				<div className="flex w-full max-w-5xl justify-between items-center px-2">
					<div className="text-xs opacity-70">Session <span className="font-mono font-semibold">{sessionid}</span></div>
					<div className="text-xs opacity-70">Game <span className="font-mono font-semibold">{gameId}</span> Round {roundIndex}</div>
					<div>
						<button type="button" className="btn btn-ghost btn-xs" onClick={() => nav(-1)}>Back</button>
					</div>
				</div>

				<CardContainer className="max-w-5xl w-full">
					<h2 className="text-lg font-semibold text-center mb-2">UNO</h2>
					{loading && <div className="text-center text-xs opacity-70">Loading...</div>}
					{error && <div className="alert alert-error py-1 px-2 text-xs mb-2">{error}</div>}
					{view && (
						<div className="flex flex-col gap-6">
							<UnoPlayerStrip players={view.players} currentPlayerId={currentPlayerId} viewerId={view.viewerId} />
							<div className="flex flex-col items-center gap-4">
								<div className="flex items-center gap-6">
									<DiscardPile top={view.top} />
									<div className="flex flex-col gap-1 text-xs">
										<div>Turn: {currentPlayerId?.slice(0,6)}</div>
										<div>Pending Draw: {pendingDraw}</div>
										<div>Must Choose Color: {mustChooseColor ? 'Yes' : 'No'}</div>
										<div>Active Color: {view.activeColor || (mustChooseColor ? '— (await)' : (view.top?.color || '—'))}</div>
										<div>Phase: {view.phase}</div>
										<div>Your Turn: {myTurn ? 'Yes' : 'No'}</div>
									</div>
								</div>
								<div className="w-full">
									<h3 className="text-sm font-semibold mb-1 text-center">Your Hand</h3>
									<UnoHand hand={hand} playableCards={playableCards} onPlay={playCard} myTurn={myTurn} mustChooseColor={mustChooseColor} />
								</div>
								<ActionPanel myTurn={myTurn} canDraw={uno.canDraw} onDraw={drawCard} onDeclareUno={declareUno} canDeclareUno={uno.canDeclareUno} pendingDraw={pendingDraw} isFinished={isFinished} sending={sending} />
								<div className="text-center text-xs opacity-70">{myTurn ? (mustChooseColor ? 'Choose a color.' : playableCards.length ? 'Select a playable card or draw.' : 'No playable card: draw a card.') : 'Waiting for opponent / bot...'}</div>
							</div>
						</div>
					)}
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
