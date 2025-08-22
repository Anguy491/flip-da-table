import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';

function Dashboard() {
	const { setToken } = useContext(AuthContext);
	const navigate = useNavigate();

	const handleLogout = () => {
		setToken(null);
		navigate('/login');
	};

	return (
		<PageContainer>
			<CardContainer>
				<h2 className="text-2xl font-bold text-center mb-4">Dashboard</h2>
				<SubmitButton type="button" onClick={handleLogout}>Logout</SubmitButton>
			</CardContainer>
		</PageContainer>
	);
}

export default Dashboard;
