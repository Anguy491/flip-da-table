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

  useEffect(() => {
    window.render_game_to_text = () => JSON.stringify({
      coordinateSystem: 'DOM table; casinos are numbered 1 through 6 and player seats follow seatIndex order',
      mode: vegas.view?.phase || 'LOADING',
      stateVersion: vegas.view?.stateVersion ?? null,
      round: vegas.view?.internalRound ?? null,
      currentPlayerId: vegas.view?.currentPlayerId ?? null,
      viewerId: vegas.playerId,
      currentRoll: vegas.view?.currentRoll || [],
      rollDialogVisible: Boolean(document.querySelector('.vegas-roll-dialog')),
      rollDialogTitle: document.querySelector('.vegas-roll-dialog .arcade-title')?.textContent || null,
      players: (vegas.view?.players || []).map((player) => ({
        playerId: player.playerId,
        name: player.name,
        bot: Boolean(player.bot),
        seatIndex: player.seatIndex,
        remainingDice: player.remainingDice,
        chips: player.chips,
        current: player.current,
      })),
      casinos: (vegas.view?.casinos || []).map((casino) => ({
        number: casino.number,
        bonuses: casino.bonuses,
        placements: casino.placements,
      })),
    });
    return () => { delete window.render_game_to_text; };
  }, [vegas.playerId, vegas.view]);

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
