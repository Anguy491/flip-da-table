import { useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import useUnoGame from '../hooks/useUnoGame';
import { AuthContext } from '../context/auth-context';
import ChooseColorModal from '../components/uno/ChooseColorModal';
import GameOverModal from '../components/uno/GameOverModal';
import UnoGameView from '../components/uno/UnoGameView';
import { ArcadeButton, ArcadeDialog, ArcadePanel } from '../components/arcade/ArcadeUI';
import { startNextGame } from '../api/sessions';
import { recordSessionResult } from '../utils/sessionResults';

export default function UnoPlayScreen() {
  const { state } = useLocation();
  const { sessionid } = useParams();
  const navigate = useNavigate();
  const { token } = useContext(AuthContext);
  const gameId = state?.gameId;
  const roundIndex = state?.roundIndex ?? 1;
  const totalRounds = state?.totalRounds ?? 1;
  const playersMeta = useMemo(() => state?.players || [], [state?.players]);
  const pastResults = useMemo(() => state?.results || [], [state?.results]);
  const playerId = state?.playerId || state?.myPlayerId || playersMeta.find((player) => !player.bot)?.playerId;
  const uno = useUnoGame({ gameId, playerId, token });
  const [gameOverOpen, setGameOverOpen] = useState(false);
  const [showRotateHint, setShowRotateHint] = useState(false);
  const [colorPickerOpen, setColorPickerOpen] = useState(false);

  useEffect(() => {
    if (gameId && !token) navigate('/login');
  }, [gameId, navigate, token]);

  const winner = useMemo(() => {
    if (uno.view?.winnerId) return uno.view.players?.find((player) => player.playerId === uno.view.winnerId) || { playerId: uno.view.winnerId };
    return uno.view?.players?.find((player) => player.handSize === 0);
  }, [uno.view]);

  useEffect(() => {
    if (winner) setGameOverOpen(true);
  }, [winner]);

  useEffect(() => {
    setColorPickerOpen(uno.mustChooseColor && uno.myTurn);
  }, [uno.mustChooseColor, uno.myTurn]);

  const winnerName = useMemo(() => {
    if (!winner) return '';
    return playersMeta.find((player) => player.playerId === winner.playerId)?.name || winner.playerId;
  }, [playersMeta, winner]);

  useEffect(() => {
    if (!winner?.playerId) return;
    recordSessionResult({
      sessionId: sessionid,
      gameType: 'UNO',
      totalRounds,
      playersMeta,
      result: { round: roundIndex, winnerId: winner.playerId, winnerName, turns: uno.view?.turnCount || 0 },
    });
  }, [playersMeta, roundIndex, sessionid, totalRounds, uno.view?.turnCount, winner, winnerName]);

  useEffect(() => {
    const evaluate = () => {
      const dismissed = sessionStorage.getItem('dismissRotateHint') === '1';
      setShowRotateHint(!dismissed && window.innerWidth < 780 && window.innerHeight > window.innerWidth);
    };
    evaluate();
    window.addEventListener('resize', evaluate);
    window.addEventListener('orientationchange', evaluate);
    return () => {
      window.removeEventListener('resize', evaluate);
      window.removeEventListener('orientationchange', evaluate);
    };
  }, []);

  const dismissRotateHint = useCallback(() => {
    sessionStorage.setItem('dismissRotateHint', '1');
    setShowRotateHint(false);
  }, []);

  const startNext = useCallback(async () => {
    if (roundIndex >= totalRounds || !winner) return;
    try {
      const payloadPlayers = playersMeta.map((player) => ({ name: player.name, bot: player.bot, ready: true }));
      const response = await startNextGame(sessionid, { rounds: totalRounds, players: payloadPlayers }, token);
      navigate(`/unoplayscreen/${sessionid}`, {
        state: {
          gameId: response.gameId,
          roundIndex: response.roundIndex,
          myPlayerId: response.myPlayerId,
          players: response.players,
          totalRounds,
          results: [...pastResults, { round: roundIndex, winnerId: winner.playerId, winnerName, turns: uno.view?.turnCount || 0 }],
        },
      });
      setGameOverOpen(false);
    } catch (requestError) {
      console.error('Unable to start next game', requestError);
    }
  }, [navigate, pastResults, playersMeta, roundIndex, sessionid, token, totalRounds, uno.view?.turnCount, winner, winnerName]);

  if (!gameId) {
    return (
      <PageContainer theme="uno">
        <ArcadePanel className="max-w-2xl mx-auto text-center">
          <p className="arcade-eyebrow">Table unavailable</p>
          <h1 className="arcade-title">No UNO game found</h1>
          <p className="arcade-copy mt-5">Launch a game from a lobby so the table receives its player perspective.</p>
          <ArcadeButton className="mt-7" onClick={() => navigate(-1)}>Back to lobby</ArcadeButton>
        </ArcadePanel>
      </PageContainer>
    );
  }

  const players = (uno.view?.players || []).map((player) => ({
    id: player.playerId,
    name: playersMeta.find((meta) => meta.playerId === player.playerId)?.name || player.playerId.slice(0, 8),
    handCount: player.handSize,
  }));
  const currentPlayerId = uno.view?.players?.find((player) => player.isCurrent)?.playerId;

  return (
    <PageContainer theme="uno" game>
      <UnoGameView
        sessionId={sessionid}
        gameId={gameId}
        players={players}
        currentPlayerId={currentPlayerId}
        round={roundIndex}
        direction={uno.view?.direction || 'CW'}
        activeColor={uno.view?.activeColor || uno.view?.top?.color}
        pendingDraw={uno.pendingDraw}
        topCard={uno.view?.top}
        events={uno.events}
        hand={uno.hand}
        playableCards={uno.playableCards}
        myTurn={uno.myTurn}
        mustChooseColor={uno.mustChooseColor}
        finished={uno.isFinished}
        sending={uno.sending}
        loading={uno.loading}
        error={uno.error}
        connectionState={uno.connectionState}
        onBack={() => navigate(-1)}
        onPlay={uno.actions.playCard}
        onDraw={uno.actions.drawCard}
        onOpenColorPicker={() => setColorPickerOpen(true)}
      />

      <ChooseColorModal
        open={colorPickerOpen && uno.mustChooseColor && uno.myTurn}
        disabled={uno.sending}
        onHide={() => setColorPickerOpen(false)}
        onPick={(color) => {
          setColorPickerOpen(false);
          uno.actions.chooseColor(color);
        }}
      />
      <GameOverModal
        open={gameOverOpen && Boolean(winner)}
        winnerName={winnerName}
        winnerId={winner?.playerId}
        turns={uno.view?.turnCount || 0}
        onClose={() => navigate('/dashboard')}
        onNext={startNext}
        isLast={roundIndex >= totalRounds}
        onSummary={() => navigate(`/sessionsum/${sessionid}`)}
      />
      <ArcadeDialog open={showRotateHint} title="Rotate to landscape" eyebrow="Better table view" onClose={dismissRotateHint}>
        <p className="arcade-copy">The full hand and opponent rail fit best when your phone is turned sideways.</p>
        <ArcadeButton className="mt-5" block onClick={dismissRotateHint}>Continue</ArcadeButton>
      </ArcadeDialog>
    </PageContainer>
  );
}
