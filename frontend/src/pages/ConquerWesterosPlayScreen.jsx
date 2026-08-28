import { useContext, useEffect } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import ConquerWesterosGameView from '../components/conquerwesteros/ConquerWesterosGameView';
import useConquerWesterosGame from '../hooks/useConquerWesterosGame';
import { AuthContext } from '../context/auth-context';

export default function ConquerWesterosPlayScreen() {
  const { state } = useLocation();
  const { sessionid } = useParams();
  const navigate = useNavigate();
  const { token } = useContext(AuthContext);
  const campaign = useConquerWesterosGame({
    sessionId: sessionid,
    initialGameId: state?.gameId,
    initialPlayerId: state?.myPlayerId,
    initialView: state?.view,
    token,
  });

  useEffect(() => {
    if (!token) navigate('/login', { replace: true });
  }, [navigate, token]);

  useEffect(() => {
    window.render_game_to_text = () => JSON.stringify({
      coordinateSystem: 'Westeros tactical map uses a 720x1000 viewBox; 14 strongholds use T01-T14 and dice use stable D1-D7 identifiers',
      mode: campaign.view?.phase || 'LOADING',
      stateVersion: campaign.view?.stateVersion ?? null,
      campaign: campaign.view?.campaign ?? null,
      currentPlayerId: campaign.view?.currentPlayerId ?? null,
      viewerId: campaign.playerId,
      ironThroneHolderId: campaign.view?.ironThroneHolderId ?? null,
      currentRoll: campaign.view?.currentRoll || [],
      attempt: campaign.view?.attempt || null,
      players: campaign.view?.players || [],
      strongholds: campaign.view?.strongholds || [],
    });
    return () => { delete window.render_game_to_text; };
  }, [campaign.playerId, campaign.view]);

  return (
    <PageContainer theme="neutral" game>
      <ConquerWesterosGameView
        view={campaign.view}
        playerId={campaign.playerId}
        loading={campaign.loading}
        sending={campaign.sending}
        error={campaign.error}
        connectionState={campaign.connectionState}
        publicEvents={campaign.publicEvents}
        onRoll={campaign.actions.roll}
        onCompleteLine={campaign.actions.completeLine}
        onLoseDie={campaign.actions.loseDie}
        onRefresh={campaign.actions.reload}
        onLeave={() => navigate('/dashboard')}
        onSummary={() => navigate(`/sessionsum/${sessionid}`)}
      />
    </PageContainer>
  );
}
