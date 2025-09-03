import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useContext, useEffect } from 'react';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import { AuthContext } from '../context/AuthContext';

export default function DVCPlayScreen() {
	const { state } = useLocation();
	const { sessionid } = useParams();
	const nav = useNavigate();
	const { token } = useContext(AuthContext);
	const gameId = state?.gameId; const roundIndex = state?.roundIndex || 1;
	const playerId = state?.playerId;

	useEffect(()=>{ if(!token) nav('/login'); },[token, nav]);

	if(!gameId) {
		return (
			<PageContainer>
				<CardContainer className="max-w-lg text-center">
					<h2 className="text-xl font-semibold mb-4">Da Vinci Code</h2>
					<div className="text-error">No game data. Start from lobby.</div>
					<SubmitButton type="button" className="btn-secondary mt-4" onClick={()=>nav(-1)}>Back</SubmitButton>
				</CardContainer>
			</PageContainer>
		);
	}

	return (
		<PageContainer>
			<CardContainer className="max-w-3xl w-full">
				<div className="flex justify-between items-center mb-2 text-xs">
					<span>Session <span className="font-mono font-semibold">{sessionid}</span></span>
					<span>Game <span className="font-mono font-semibold">{gameId}</span></span>
					<button type="button" className="btn btn-ghost btn-xs" onClick={()=>nav(-1)}>Back</button>
				</div>
				<h2 className="text-lg font-bold mb-4">Da Vinci Code (Round {roundIndex})</h2>
				<div className="alert alert-info text-xs mb-4">Prototype DVC play screen. Gameplay interactions TBD.</div>
				<div className="text-sm">Player: <span className="font-mono">{playerId}</span></div>
			</CardContainer>
		</PageContainer>
	);
}
