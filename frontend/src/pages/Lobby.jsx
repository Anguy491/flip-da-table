import { useParams, useNavigate } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import CardContainer from '../components/CardContainer';
import SubmitButton from '../components/SubmitButton';

export default function Lobby() {
  const { sessionid } = useParams();
  const nav = useNavigate();
  return (
    <PageContainer>
      <CardContainer className="max-w-xl text-center">
        <h2 className="text-xl font-semibold mb-4">Lobby</h2>
        <div className="mb-6">Session ID: <span className="font-mono font-bold">{sessionid}</span></div>
        <SubmitButton type="button" className="btn-secondary" onClick={() => nav(-1)}>Back</SubmitButton>
      </CardContainer>
    </PageContainer>
  );
}
