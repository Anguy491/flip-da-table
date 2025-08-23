import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';

export default function PlayScreen() {
	const { state } = useLocation();
	const { sessionid } = useParams();
	const nav = useNavigate();
	const gameId = state?.gameId;
	const roundIndex = state?.roundIndex;

	return (
		<PageContainer>
			<CardContainer className="max-w-xl w-full text-center">
				<h2 className="text-xl font-semibold mb-4">Play Screen</h2>
				<div className="space-y-2">
					<div>Session: <span className="font-mono font-bold">{sessionid}</span></div>
					{gameId ? (
						<>
							<div>Game ID: <span className="font-mono font-bold">{gameId}</span></div>
							<div>Round: {roundIndex}</div>
						</>
					) : (
						<div className="text-error">No game started data found.</div>
					)}
				</div>
				<SubmitButton type="button" className="btn-secondary mt-4" onClick={() => nav(-1)}>Back</SubmitButton>
			</CardContainer>
		</PageContainer>
	);
}
