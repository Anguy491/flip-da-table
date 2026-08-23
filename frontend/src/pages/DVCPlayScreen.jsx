import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useContext, useEffect, useState, useCallback } from 'react';
import PageContainer from '../components/PageContainer';
import { AuthContext } from '../context/auth-context';
import { fetchDvcView, drawColor, guess as apiGuess, revealDecision, selfReveal, settle } from '../api/dvc';
import { useDVCGame } from '../hooks/useDVCGame';
import { isArrangementValid } from '../components/dvc/arrangement';
import { GuessModal } from '../components/dvc/GuessModal';
import { InsertPreviewModal } from '../components/dvc/InsertPreviewModal';
import DvcGameOverModal from '../components/dvc/GameOverModal';
import DvcGameView from '../components/dvc/DvcGameView';
import { ArcadeButton, ArcadeDialog, ArcadePanel } from '../components/arcade/ArcadeUI';
import { recordSessionResult } from '../utils/sessionResults';

const EMPTY_PLAYERS = [];

export default function DVCPlayScreen({ initial }) {
  const { state } = useLocation();
  const { sessionid } = useParams();
  const navigate = useNavigate();
  const { token } = useContext(AuthContext);
  const base = initial || state;
  const gameId = base?.gameId;
  const roundIndex = base?.roundIndex || 1;
  const totalRounds = base?.totalRounds || 1;
  const playersMeta = base?.players || EMPTY_PLAYERS;
  const myPlayerId = base?.myPlayerId;

  const [view, setView] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [loadingAction, setLoadingAction] = useState(false);
  const [connectionState, setConnectionState] = useState('connecting');
  const [showGuess, setShowGuess] = useState(false);
  const [showInsert, setShowInsert] = useState(false);
  const [showRotateHint, setShowRotateHint] = useState(false);
  const [lastGuessCorrect, setLastGuessCorrect] = useState(false);
  const [settledSubmitted, setSettledSubmitted] = useState(false);
  const [guessForm, setGuessForm] = useState({ targetPlayerId: '', targetIndex: 0, guessColor: 'BLACK', guessValue: '0', joker: false });
  const [pendingCard, setPendingCard] = useState(null);
  const [publicTokens, setPublicTokens] = useState(new Set());
  const [selfRevealIndex, setSelfRevealIndex] = useState(null);

  useEffect(() => {
    if (!token) navigate('/login');
  }, [navigate, token]);

  const refreshView = useCallback(async () => {
    if (!gameId || !myPlayerId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const nextView = await fetchDvcView(gameId, myPlayerId, token);
      setView(nextView);
      const mine = nextView?.players?.find((player) => player.playerId === myPlayerId);
      setPendingCard(mine?.pending || null);
      setError('');

      const response = await fetch(`/api/dvc/${gameId}/public-tokens`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();
        setPublicTokens(new Set(data?.[myPlayerId] || []));
      }
    } catch (requestError) {
      setError(requestError.message || 'Failed to sync the code table.');
    } finally {
      setLoading(false);
    }
  }, [gameId, myPlayerId, token]);

  useEffect(() => { void refreshView(); }, [refreshView]);

  useEffect(() => {
    if (!gameId || !myPlayerId) {
      setConnectionState('offline');
      return undefined;
    }
    let client;
    let active = true;
    setConnectionState('connecting');

    import('@stomp/stompjs').then(({ Client }) => {
      if (!active) return;
      client = new Client({
        brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 3000,
        onConnect: () => {
          if (!active) return;
          setConnectionState('connected');
          client.subscribe(`/topic/dvc/${gameId}/${myPlayerId}`, (message) => {
            try {
              const payload = JSON.parse(message.body);
              setView(payload);
              setPendingCard(payload?.players?.find((player) => player.playerId === myPlayerId)?.pending || null);
              setConnectionState('connected');
            } catch {
              setError('A private table update could not be read.');
            }
          });
          client.subscribe(`/topic/dvc/${gameId}/public-reveals`, (message) => {
            try {
              const events = JSON.parse(message.body);
              if (!Array.isArray(events)) return;
              setPublicTokens((current) => {
                const next = new Set(current);
                for (const event of events) if (event?.playerId === myPlayerId && event?.token) next.add(event.token);
                return next;
              });
            } catch {
              setError('A public reveal update could not be read.');
            }
          });
        },
        onWebSocketClose: () => { if (active) setConnectionState('reconnecting'); },
        onStompError: () => { if (active) setConnectionState('offline'); },
      });
      client.activate();
    }).catch(() => setConnectionState('offline'));

    return () => {
      active = false;
      if (client) void client.deactivate();
    };
  }, [gameId, myPlayerId, token]);

  useEffect(() => {
    const evaluate = () => {
      const dismissed = sessionStorage.getItem('dismissDvcRotateHint') === '1';
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

  const game = useDVCGame({ view, myPlayerId });
  const { board, awaiting, parsedHand: myCards, isMyTurn, reorderHand, canDragInitial } = game;
  const playerViews = view?.players || [];
  const currentPlayerId = board ? playerViews[board.currentPlayerIndex]?.playerId : null;
  const arrangementValid = awaiting === 'SETTLE_POSITION' ? isArrangementValid(myCards) : true;
  const isStartPhaseSettle = awaiting === 'SETTLE_POSITION' && !pendingCard;
  const disabled = awaiting === 'SETTLE_POSITION'
    ? Boolean(board?.winnerId || loadingAction)
    : Boolean(!isMyTurn || board?.winnerId || loadingAction);
  const winnerId = board?.winnerId;
  const winnerName = playersMeta.find((player) => player.playerId === winnerId)?.name || winnerId;

  useEffect(() => {
    if (!winnerId) return;
    recordSessionResult({
      sessionId: sessionid,
      gameType: 'DAVINCI',
      totalRounds,
      playersMeta,
      result: { round: roundIndex, winnerId, winnerName, turns: board?.turnId || 0 },
    });
  }, [board?.turnId, playersMeta, roundIndex, sessionid, totalRounds, winnerId, winnerName]);

  useEffect(() => {
    if (awaiting !== 'SELF_REVEAL_CHOICE') setSelfRevealIndex(null);
    if (awaiting !== 'SETTLE_POSITION') setSettledSubmitted(false);
  }, [awaiting]);

  const doDrawColor = async (color) => {
    if (disabled || awaiting !== 'DRAW_COLOR') return;
    setLoadingAction(true);
    setError('');
    try {
      await drawColor(gameId, myPlayerId, color, token);
      await refreshView();
    } catch (requestError) {
      setError(requestError.message || 'Draw failed.');
    } finally {
      setLoadingAction(false);
    }
  };

  const submitGuess = async () => {
    if (disabled || awaiting !== 'GUESS_SELECTION') return;
    setLoadingAction(true);
    setError('');
    try {
      const joker = guessForm.joker || guessForm.guessValue === '_';
      const number = joker ? null : Number(guessForm.guessValue);
      const correct = await apiGuess(gameId, myPlayerId, guessForm.targetPlayerId, Number(guessForm.targetIndex), joker, number, token);
      setLastGuessCorrect(Boolean(correct));
      setShowGuess(false);
      await refreshView();
    } catch (requestError) {
      setError(requestError.message || 'Guess failed.');
    } finally {
      setLoadingAction(false);
    }
  };

  const continueReveal = async (continueRun) => {
    if (disabled || awaiting !== 'REVEAL_DECISION') return;
    setLoadingAction(true);
    setError('');
    try {
      await revealDecision(gameId, myPlayerId, continueRun, token);
      await refreshView();
    } catch (requestError) {
      setError(requestError.message || 'Decision failed.');
    } finally {
      setLoadingAction(false);
    }
  };

  const doSelfReveal = async () => {
    if (disabled || awaiting !== 'SELF_REVEAL_CHOICE' || selfRevealIndex == null) return;
    setLoadingAction(true);
    setError('');
    try {
      await selfReveal(gameId, myPlayerId, selfRevealIndex, token);
      await refreshView();
    } catch (requestError) {
      setError(requestError.message || 'Reveal failed.');
    } finally {
      setLoadingAction(false);
    }
  };

  const doSettle = async (handOverride) => {
    if (disabled || awaiting !== 'SETTLE_POSITION') return;
    setLoadingAction(true);
    setError('');
    try {
      const hand = handOverride ?? myCards.map((card) => `${card.color === 'BLACK' ? 'B' : 'W'}${card.isJoker || card.value === '-' ? '_' : card.value}≤`).join('');
      await settle(gameId, myPlayerId, hand, true, token);
      setSettledSubmitted(true);
      setShowInsert(false);
      await refreshView();
    } catch (requestError) {
      setError(requestError.message || 'Settle failed.');
    } finally {
      setLoadingAction(false);
    }
  };

  if (!gameId) {
    return (
      <PageContainer theme="dvc">
        <ArcadePanel className="max-w-2xl mx-auto text-center">
          <p className="arcade-eyebrow">Table unavailable</p>
          <h1 className="arcade-title">No code game found</h1>
          <p className="arcade-copy mt-5">Launch Da Vinci Code from a lobby to receive your private rack.</p>
          <ArcadeButton className="mt-7" onClick={() => navigate(-1)}>Back to lobby</ArcadeButton>
        </ArcadePanel>
      </PageContainer>
    );
  }

  return (
    <PageContainer theme="dvc" game>
      <DvcGameView
        sessionId={sessionid}
        gameId={gameId}
        playerViews={playerViews}
        myPlayerId={myPlayerId}
        currentPlayerId={currentPlayerId}
        board={board}
        awaiting={awaiting}
        roundIndex={roundIndex}
        myCards={myCards}
        pendingCard={pendingCard}
        publicTokens={publicTokens}
        canDragInitial={canDragInitial}
        arrangementValid={arrangementValid}
        isMyTurn={isMyTurn}
        disabled={disabled}
        loading={loading}
        error={error}
        connectionState={connectionState}
        selectedIndex={selfRevealIndex}
        settledSubmitted={settledSubmitted}
        lastGuessCorrect={lastGuessCorrect}
        actionLog={view?.actionLog || []}
        onSelectSelf={setSelfRevealIndex}
        onReorder={reorderHand}
        onOpponentCardClick={(targetPlayerId, targetIndex) => {
          if (awaiting !== 'GUESS_SELECTION' || !isMyTurn) return;
          setGuessForm((current) => ({ ...current, targetPlayerId, targetIndex }));
          setShowGuess(true);
        }}
        onBack={() => navigate(-1)}
        onRefresh={() => void refreshView()}
        onDrawColor={(color) => void doDrawColor(color)}
        onContinueReveal={(continueRun) => void continueReveal(continueRun)}
        onSelfReveal={() => void doSelfReveal()}
        onSettle={() => { if (isStartPhaseSettle) void doSettle(); else setShowInsert(true); }}
      />

      <DvcGameOverModal
        open={Boolean(board?.winnerId)}
        winnerName={winnerName}
        winnerId={winnerId}
        turns={board?.turnId}
        onClose={() => navigate('/dashboard')}
        onSummary={() => navigate(`/sessionsum/${sessionid}`)}
      />
      <GuessModal open={showGuess} guessForm={guessForm} setGuessForm={setGuessForm} onSubmit={() => void submitGuess()} onClose={() => setShowGuess(false)} />
      <InsertPreviewModal open={showInsert} myCards={myCards} pending={pendingCard} onClose={() => setShowInsert(false)} onConfirm={(handString) => void doSettle(handString)} />
      <ArcadeDialog
        open={showRotateHint}
        title="Rotate to landscape"
        eyebrow="Better rack view"
        onClose={() => {
          sessionStorage.setItem('dismissDvcRotateHint', '1');
          setShowRotateHint(false);
        }}
      >
        <p className="arcade-copy">Turn your phone sideways to compare every code rack without hiding the controls.</p>
        <ArcadeButton className="mt-5" block onClick={() => {
          sessionStorage.setItem('dismissDvcRotateHint', '1');
          setShowRotateHint(false);
        }}>Continue</ArcadeButton>
      </ArcadeDialog>
    </PageContainer>
  );
}
