import { useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { createSession, joinSession } from '../api/sessions';
import ImgDaVinci from '../assets/Davinci.png';
import ImgUno from '../assets/Uno.png';
import ImgBounty from '../assets/Bounty.png';

const GAMES = [
	{
		gameType: 'DAVINCI',
		name: 'Da Vinci Code',
		players: '2-4',
		maxPlayers: 4,
		img: ImgDaVinci,
	},
	{
		gameType: 'UNO',
		name: 'UNO',
		players: '2-10',
		maxPlayers: 10,
		img: ImgUno,
	},
	{
		gameType: 'BOUNTY',
		name: "Bounty N' Booty",
		players: '2-6',
		maxPlayers: 6,
		img: ImgBounty,
	},
];

function Dashboard() {
	const { token, setToken } = useContext(AuthContext);
	const navigate = useNavigate();
	const [showModal, setShowModal] = useState(false);
	const [showJoin, setShowJoin] = useState(false);
	const [joinSessionId, setJoinSessionId] = useState('');
	const [selected, setSelected] = useState(null);
	const [submitting, setSubmitting] = useState(false);
	const [error, setError] = useState('');

	useEffect(() => {
		if (!token) {
			navigate('/login');
		}
	}, [token, navigate]);

	const handleLogout = () => {
		setToken(null);
		navigate('/login');
	};

	const openModal = () => {
		setError('');
		setSelected(null);
		setShowModal(true);
	};

	const openJoin = () => {
		setError('');
		setJoinSessionId('');
		setShowJoin(true);
	};

	const handleJoin = async () => {
		const id = joinSessionId.trim();
		if (!id) return;
		try {
			await joinSession(id, token);
			setShowJoin(false);
			navigate(`/lobby/${id}`);
		} catch (e) {
			setError(e.message || 'Failed to join session');
		}
	};

	const handleConfirm = async () => {
		if (!selected || !token) return;
		setSubmitting(true);
		setError('');
		try {
			const { sessionId } = await createSession({ gameType: selected.gameType, maxPlayers: selected.maxPlayers }, token);
			await joinSession(sessionId, token);
			setShowModal(false);
			navigate(`/lobby/${sessionId}`);
		} catch (e) {
			setError(e.message || 'Failed to create or join session');
		} finally {
			setSubmitting(false);
		}
	};

	return (
		<PageContainer>
			<CardContainer className="max-w-xl">
				<h2 className="text-2xl font-bold text-center mb-4">Dashboard</h2>
				<div className="flex flex-col gap-3 justify-center">
					<SubmitButton type="button" className="btn-secondary" onClick={openModal}>Create Session</SubmitButton>
					<SubmitButton type="button" className="btn-secondary" onClick={openJoin}>Join Session</SubmitButton>
					<SubmitButton type="button" className="btn-ghost" onClick={handleLogout}>Logout</SubmitButton>
				</div>
				<ErrorPopup message={error} />
			</CardContainer>

					{showModal && (
						<div className="fixed inset-0 flex items-center justify-center bg-black/50 p-4 z-50">
							<div className="bg-base-100 rounded-lg shadow-xl w-full max-w-5xl p-8 flex flex-col gap-6 max-h-[90vh]">
								<h3 className="text-2xl font-semibold">Select a Game</h3>
								<div className="overflow-y-auto pr-2">
									<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
										{GAMES.map(g => {
											const active = selected?.gameType === g.gameType;
											return (
												<button
													key={g.gameType}
													type="button"
													onClick={() => setSelected(g)}
													className={`border rounded-xl p-4 flex flex-col items-center gap-3 hover:border-primary transition h-72 bg-base-200/40 ${active ? 'border-primary ring ring-primary/40' : 'border-base-300'}`}
												>
													<img src={g.img} alt={g.name} className="w-full h-44 object-cover rounded-md shadow-sm" />
													<div className="text-base font-medium text-center">{g.name}</div>
													<div className="text-xs opacity-70">Players: {g.players}</div>
												</button>
											);
										})}
									</div>
								</div>
								<div className="flex justify-end gap-4 pt-2">
									<SubmitButton type="button" className="btn-ghost" onClick={() => setShowModal(false)} disabled={submitting}>Cancel</SubmitButton>
									<SubmitButton type="button" disabled={!selected || submitting} onClick={handleConfirm}>
										{submitting ? 'Creating...' : 'Confirm'}
									</SubmitButton>
								</div>
							</div>
						</div>
					)}

					{showJoin && (
						<div className="fixed inset-0 flex items-center justify-center bg-black/50 p-4 z-50">
							<div className="bg-base-100 rounded-lg shadow-xl w-full max-w-sm p-6 flex flex-col gap-4">
								<h3 className="text-xl font-semibold">Join Session</h3>
								<input
									className="input input-bordered w-full"
									placeholder="Enter Session ID"
									value={joinSessionId}
									onChange={e=>setJoinSessionId(e.target.value)}
								/>
								<div className="flex justify-end gap-3 pt-2">
									<button type="button" className="btn btn-ghost" onClick={()=>setShowJoin(false)}>Cancel</button>
									<button type="button" className="btn btn-primary" disabled={!joinSessionId.trim()} onClick={handleJoin}>Join</button>
								</div>
							</div>
						</div>
					)}
		</PageContainer>
	);
}

export default Dashboard;
