import { useContext, useEffect } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import LasVegasGameView from '../components/lasvegas/LasVegasGameView';
import useLasVegasGame from '../hooks/useLasVegasGame';
import { AuthContext } from '../context/auth-context';

export default function LasVegasPlayScreen() {
  const { state } = useLocation();
  const { sessionid } = useParams();
  const navigate = useNavigate();
  const { token } = useContext(AuthContext);
  const vegas = useLasVegasGame({
    sessionId: sessionid,
    initialGameId: state?.gameId,
    initialPlayerId: state?.myPlayerId,
    initialView: state?.view,
    token,
  });

  useEffect(() => {
    if (!token) navigate('/login', { replace: true });
  }, [navigate, token]);

  return (
    <PageContainer theme="vegas" game>
      <LasVegasGameView
        sessionId={sessionid}
        gameId={vegas.gameId}
        view={vegas.view}
        playerId={vegas.playerId}
        loading={vegas.loading}
        sending={vegas.sending}
        error={vegas.error}
        connectionState={vegas.connectionState}
        publicEvents={vegas.publicEvents}
        assetsVisible={vegas.assetsVisible}
        onRoll={vegas.actions.roll}
        onPlace={vegas.actions.place}
        onSkip={vegas.actions.skip}
        onToggleAssets={vegas.actions.setAssetsVisible}
        onRefresh={vegas.actions.reload}
        onLeave={() => navigate('/dashboard')}
        onSummary={() => navigate(`/sessionsum/${sessionid}`)}
      />
    </PageContainer>
  );
}
