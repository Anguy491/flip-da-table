import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import {
  ArcadeBadge,
  ArcadeButton,
  ArcadeDialog,
  ArcadeInput,
  ArcadePanel,
  ArcadeSelect,
  ConnectionBadge,
  PlayerSeat,
  StatusBanner,
} from '../components/arcade/ArcadeUI';
import UnoGameView from '../components/uno/UnoGameView';
import ChooseColorModal from '../components/uno/ChooseColorModal';
import DvcGameView from '../components/dvc/DvcGameView';
import LasVegasGameView from '../components/lasvegas/LasVegasGameView';
import ConquerWesterosGameView from '../components/conquerwesteros/ConquerWesterosGameView';
import Login from './Login';
import Register from './Register';
import Dashboard from './Dashboard';
import Lobby from './Lobby';
import SessionSummary from './SessionSummary';
import ForgotPassword from './ForgotPassword';
import ResetPassword from './ResetPassword';
import Privacy from './Privacy';
import {
  dashboardFixture,
  conquerWesterosAegonFixture,
  conquerWesterosDanceFixture,
  conquerWesterosFixture,
  conquerWesterosUsurperFixture,
  dvcFixture,
  lobbyFixture,
  lasVegasFixture,
  summaryFixture,
  unoFixture,
} from '../dev/fixtures';

const tokens = ['ink', 'surface', 'panel', 'cyan', 'magenta', 'yellow', 'error', 'success'];
const authPreviewCapabilities = {
  passwordReset: true,
  supportEmail: 'support@anguy.dev',
  google: { enabled: false, clientId: '', loginUri: '' },
};

export default function UILab() {
  const [params] = useSearchParams();
  const screen = params.get('screen') || 'components';
  const wildMode = params.get('state') === 'wild';
  const dvcSettledMode = params.get('state') === 'settled';
  const vegasBotMode = params.get('state') === 'bot';
  const vegasBotSequenceMode = params.get('state') === 'bot-sequence';
  const vegasCrowdedMode = params.get('state') === 'crowded';
  const vegasRollMode = params.get('state') === 'roll';
  const conquerState = params.get('state') || 'unlocked-target';
  const conquerCampaign = params.get('campaign');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [colorPickerOpen, setColorPickerOpen] = useState(wildMode);
  const [vegasBotStep, setVegasBotStep] = useState(0);
  const [vegasRollStep, setVegasRollStep] = useState(0);
  const theme = screen === 'uno' ? 'uno' : screen === 'dvc' ? 'dvc' : screen === 'vegas' ? 'vegas' : 'neutral';
  const vegasView = useMemo(() => {
    if (vegasRollMode) {
      const currentRoll = [
        { face: 6, big: false },
        { face: 2, big: false },
        { face: 5, big: false },
        { face: 1, big: false },
        { face: 4, big: false },
        { face: 3, big: false },
        { face: 6, big: false },
        { face: 5, big: true },
      ];
      const rollAccepted = vegasRollStep >= 2;
      return {
        ...lasVegasFixture,
        phase: vegasRollStep === 0 || rollAccepted ? 'WAITING_FOR_ROLL' : 'WAITING_FOR_CHOICE',
        stateVersion: 40 + vegasRollStep,
        currentPlayerId: rollAccepted ? 'P2' : 'P1',
        currentRoll: vegasRollStep === 1 ? currentRoll : [],
        players: lasVegasFixture.players.map((player) => player.playerId === 'P1'
          ? {
            ...player,
            current: !rollAccepted,
            remainingRegularDice: 7,
            bigDieRemaining: true,
            remainingDice: 8,
          }
          : { ...player, current: rollAccepted && player.playerId === 'P2' }),
      };
    }
    if (vegasBotSequenceMode) {
      const states = [
        { currentPlayerId: 'BOT1', phase: 'WAITING_FOR_ROLL', currentRoll: [] },
        { currentPlayerId: 'BOT1', phase: 'WAITING_FOR_CHOICE', currentRoll: [{ face: 1, big: false }, { face: 1, big: true }, { face: 3, big: false }] },
        { currentPlayerId: 'BOT2', phase: 'WAITING_FOR_ROLL', currentRoll: [] },
        { currentPlayerId: 'BOT2', phase: 'WAITING_FOR_CHOICE', currentRoll: [{ face: 2, big: false }, { face: 4, big: false }] },
        { currentPlayerId: 'P1', phase: 'WAITING_FOR_ROLL', currentRoll: [] },
      ];
      const state = states[vegasBotStep];
      const players = [
        { ...lasVegasFixture.players[0], playerId: 'P1', name: 'PixelPilot', bot: false, seatIndex: 0 },
        { ...lasVegasFixture.players[9], playerId: 'BOT1', name: 'Bot 1', bot: true, seatIndex: 1 },
        { ...lasVegasFixture.players[9], playerId: 'BOT2', name: 'Bot 2', bot: true, seatIndex: 2 },
      ].map((player) => ({ ...player, current: player.playerId === state.currentPlayerId }));
      return {
        ...lasVegasFixture,
        ...state,
        stateVersion: 20 + vegasBotStep,
        turnCount: lasVegasFixture.turnCount + (vegasBotStep >= 2 ? 1 : 0),
        players,
        casinos: lasVegasFixture.casinos.map((casino) => ({
          ...casino,
          placements: casino.number === 1 && vegasBotStep >= 2
            ? [{ playerId: 'BOT1', regularDice: 1, bigDie: true, influence: 3 }]
            : [],
        })),
      };
    }
    if (vegasCrowdedMode) {
      return {
        ...lasVegasFixture,
        casinos: lasVegasFixture.casinos.map((casino) => casino.number === 1
          ? {
            ...casino,
            placements: lasVegasFixture.players.slice(0, 8).map((player, index) => ({
              playerId: player.playerId,
              regularDice: (index % 3) + 1,
              bigDie: index % 2 === 0,
              influence: (index % 3) + 1 + (index % 2 === 0 ? 2 : 0),
            })),
          }
          : casino),
      };
    }
    return vegasBotMode ? {
      ...lasVegasFixture,
      currentPlayerId: 'BOT1',
      players: lasVegasFixture.players.map((player) => ({
        ...player,
        current: player.playerId === 'BOT1',
      })),
    } : lasVegasFixture;
  }, [vegasBotMode, vegasBotSequenceMode, vegasBotStep, vegasCrowdedMode, vegasRollMode, vegasRollStep]);
  const conquerView = useMemo(() => {
    const base = {
      dance: conquerWesterosDanceFixture,
      usurper: conquerWesterosUsurperFixture,
      conquest: conquerWesterosAegonFixture,
    }[conquerCampaign] || conquerWesterosFixture;
    if (conquerState === 'bot-turn') return {
      ...base,
      currentPlayerId: 'BOT1',
      players: base.players.map((player) => ({ ...player, current: player.playerId === 'BOT1' })),
      legalActions: { canRoll: false, canCompleteLine: false, canLoseDie: false, legalTargetIds: [], legalDieIds: [] },
    };
    if (conquerState === 'waiting-to-roll') return {
      ...base,
      phase: 'WAITING_FOR_ROLL',
      currentRoll: [],
      legalActions: { ...base.legalActions, canRoll: true, canCompleteLine: false, canLoseDie: false, legalDieIds: [] },
    };
    if (conquerState === 'partial-siege') {
      const card = base.strongholds.find((item) => item.id === 'T05');
      return {
        ...base,
        stateVersion: 9,
        currentRoll: base.currentRoll.filter((die) => die.dieId >= 2),
        attempt: {
          targetId: 'T05', targetOwnerId: null, stealing: false, completedLineIds: ['L1'], lostDieIds: [], committedDieIds: [0, 1],
          requiredLines: card.lines.map((line) => ({ ...line, completed: line.id === 'L1' })),
        },
      };
    }
    if (conquerState === 'double-crown') {
      const card = base.strongholds.find((item) => item.id === 'T10');
      const stealLine = { id: 'STEAL_CROWN', type: 'STEAL_CROWN', threshold: null, symbols: ['CROWN'], display: 'Crown', completed: false, special: true };
      return {
        ...base,
        stateVersion: 11,
        currentRoll: base.currentRoll.filter((die) => [4, 5, 6].includes(die.dieId)),
        attempt: {
          targetId: 'T10', targetOwnerId: 'P2', stealing: true, completedLineIds: ['L1', 'L2'], lostDieIds: [], committedDieIds: [0, 1, 2, 3],
          requiredLines: [...card.lines.map((line) => ({ ...line, completed: ['L1', 'L2'].includes(line.id) })), stealLine],
        },
        strongholds: base.strongholds.map((item) => item.id === 'T10'
          ? { ...item, stealCrownRequired: true, lines: [...item.lines, stealLine] }
          : item),
        legalActions: { ...base.legalActions, legalDieIds: [4, 5, 6] },
      };
    }
    if (conquerState === 'clan-locked') return {
      ...base,
      strongholds: base.strongholds.map((item) => item.id === 'T13'
        ? { ...item, ownerId: 'P1', central: false, locked: true }
        : item),
      players: base.players.map((player) => player.playerId === 'P1' ? {
        ...player,
        completedClans: [{ name: 'Arryn', score: 3, strongholdIds: ['T13'] }],
        strongholdCount: 1,
        completedClanCount: 1,
        clanScore: 3,
        totalScore: 3,
      } : player),
      legalActions: { ...base.legalActions, legalTargetIds: base.legalActions.legalTargetIds.filter((id) => id !== 'T13') },
    };
    if (conquerState === 'finished') return {
      ...base,
      phase: 'FINISHED',
      currentRoll: [],
      legalActions: { canRoll: false, canCompleteLine: false, canLoseDie: false, legalTargetIds: [], legalDieIds: [] },
      results: [
        { playerId: 'P2', name: 'CipherFox', rank: 1, totalScore: 18, faceUpScore: 8, clanScore: 9, thronePoint: 1, strongholdCount: 7, completedClanCount: 2, winner: true },
        { playerId: 'P1', name: 'PixelPilot', rank: 2, totalScore: 14, faceUpScore: 4, clanScore: 10, thronePoint: 0, strongholdCount: 5, completedClanCount: 1, winner: false },
        { playerId: 'P3', name: 'LongNicknameThatNeedsTruncation', rank: 3, totalScore: 6, faceUpScore: 6, clanScore: 0, thronePoint: 0, strongholdCount: 2, completedClanCount: 0, winner: false },
      ],
    };
    return base;
  }, [conquerCampaign, conquerState]);

  useEffect(() => {
    if (screen !== 'vegas') return undefined;
    window.render_game_to_text = () => JSON.stringify({
      coordinateSystem: 'DOM table; casinos 1-6 and seats ordered by seatIndex',
      mode: vegasView.phase,
      stateVersion: vegasView.stateVersion,
      currentPlayerId: vegasView.currentPlayerId,
      currentRoll: vegasView.currentRoll,
      rollDialogVisible: Boolean(document.querySelector('.vegas-roll-dialog')),
      rollDialogTitle: document.querySelector('.vegas-roll-dialog .arcade-title')?.textContent || null,
      players: vegasView.players.map(({ playerId, name, bot, current, remainingDice, chips }) => ({
        playerId, name, bot, current, remainingDice, chips,
      })),
      casinos: vegasView.casinos,
    });
    if (vegasBotSequenceMode) {
      window.advance_las_vegas_bot_fixture = () => setVegasBotStep((step) => Math.min(step + 1, 4));
    }
    return () => {
      delete window.render_game_to_text;
      delete window.advance_las_vegas_bot_fixture;
    };
  }, [screen, vegasBotSequenceMode, vegasView]);

  useEffect(() => {
    if (screen !== 'conquer') return undefined;
    window.render_game_to_text = () => JSON.stringify({
      coordinateSystem: 'Westeros tactical map uses a 720x1000 viewBox; strongholds T01-T14 and dice D1-D7 keep stable identifiers',
      fixtureState: conquerState,
      mode: conquerView.phase,
      stateVersion: conquerView.stateVersion,
      currentPlayerId: conquerView.currentPlayerId,
      ironThroneHolderId: conquerView.ironThroneHolderId,
      currentRoll: conquerView.currentRoll,
      attempt: conquerView.attempt,
      players: conquerView.players,
      strongholds: conquerView.strongholds,
    });
    return () => { delete window.render_game_to_text; };
  }, [conquerState, conquerView, screen]);

  if (screen === 'login') return <Login previewCapabilities={authPreviewCapabilities} />;
  if (screen === 'register') return <Register previewCapabilities={authPreviewCapabilities} />;
  if (screen === 'forgot') return <ForgotPassword />;
  if (screen === 'reset') return <ResetPassword previewToken="preview-reset-token" />;
  if (screen === 'privacy') return <Privacy previewCapabilities={authPreviewCapabilities} />;
  if (screen === 'dashboard') return <Dashboard preview={dashboardFixture} />;
  if (screen === 'lobby') return <Lobby preview={lobbyFixture} />;
  if (screen === 'summary') return <SessionSummary previewData={summaryFixture} previewSessionId="ARCADE-ROOM-8BIT-2048" />;

  return (
    <PageContainer theme={theme} game={screen !== 'components'}>
      {screen === 'components' && (
        <nav className="arcade-actions mb-6" aria-label="UI Lab views">
          <Link className="arcade-button arcade-button--small" to="/__ui-lab?screen=components">Components</Link>
          <Link className="arcade-button arcade-button--small arcade-button--secondary" to="/__ui-lab?screen=uno">UNO table</Link>
          <Link className="arcade-button arcade-button--small arcade-button--ghost" to="/__ui-lab?screen=dvc">DVC table</Link>
        </nav>
      )}

      {screen === 'uno' && (
        <>
          <UnoGameView
            {...unoFixture}
            playableCards={wildMode ? [] : [unoFixture.hand[0], unoFixture.hand[4], unoFixture.hand[5]]}
            myTurn
            mustChooseColor={wildMode}
            connectionState="reconnecting"
            onBack={() => {}}
            onPlay={() => {}}
            onDraw={() => {}}
            onOpenColorPicker={() => setColorPickerOpen(true)}
          />
          <ChooseColorModal
            open={wildMode && colorPickerOpen}
            onHide={() => setColorPickerOpen(false)}
            onPick={() => setColorPickerOpen(false)}
          />
        </>
      )}

      {screen === 'dvc' && (
        <DvcGameView
          {...dvcFixture}
          awaiting={dvcSettledMode ? 'SETTLE_POSITION' : dvcFixture.awaiting}
          board={dvcSettledMode ? { ...dvcFixture.board, awaiting: 'SETTLE_POSITION' } : dvcFixture.board}
          canDragInitial={dvcSettledMode}
          settledSubmitted={dvcSettledMode}
          publicTokens={new Set()}
          arrangementValid
          isMyTurn
          disabled={false}
          connectionState="connected"
          onSelectSelf={() => {}}
          onReorder={() => {}}
          onOpponentCardClick={() => {}}
          onBack={() => {}}
          onRefresh={() => {}}
          onDrawColor={() => {}}
          onContinueReveal={() => {}}
          onSelfReveal={() => {}}
          onSettle={() => {}}
        />
      )}

      {screen === 'vegas' && (
        <LasVegasGameView
          sessionId="VEGAS-10-SEATS"
          gameId="LASVEGAS-DEMO-01"
          view={vegasView}
          playerId="P1"
          connectionState="connected"
          loading={false}
          sending={false}
          error=""
          publicEvents={[{
            sequence: 12,
            type: 'CASINO_JACKPOT',
            actorId: 'P5',
            casinoNumber: 6,
            amount: 100_000,
          }]}
          assetsVisible={false}
          onRoll={() => { if (vegasRollMode) setVegasRollStep(1); }}
          onPlace={() => { if (vegasRollMode) setVegasRollStep(2); }}
          onSkip={() => { if (vegasRollMode) setVegasRollStep(2); }}
          onToggleAssets={() => {}}
          onRefresh={() => {}}
          onLeave={() => {}}
          onSummary={() => {}}
        />
      )}

      {screen === 'conquer' && (
        <ConquerWesterosGameView
          view={['loading', 'error'].includes(conquerState) ? null : conquerView}
          playerId="P1"
          connectionState={conquerState === 'reconnecting' ? 'reconnecting' : 'connected'}
          loading={conquerState === 'loading'}
          sending={false}
          error={conquerState === 'error' ? 'The campaign snapshot could not be loaded.' : ''}
          publicEvents={[]}
          onRoll={() => {}}
          onCompleteLine={() => {}}
          onLoseDie={() => {}}
          onRefresh={() => {}}
          onLeave={() => {}}
          onSummary={() => {}}
        />
      )}

      {screen === 'components' && (
        <div className="arcade-dashboard-layout">
          <header>
            <p className="arcade-eyebrow">Development only // deterministic states</p>
            <h1 className="arcade-title">Arcade UI Lab</h1>
            <p className="arcade-copy mt-3">Canonical primitives and states used by visual regression tests.</p>
          </header>

          <ArcadePanel aria-labelledby="tokens-title">
            <h2 id="tokens-title" className="text-xl font-bold mb-5">Color tokens</h2>
            <div className="arcade-token-grid">
              {tokens.map((token) => <div className="arcade-token" data-token={token} key={token}><div className="arcade-token__swatch" /><span className="arcade-code">{token}</span></div>)}
            </div>
          </ArcadePanel>

          <ArcadePanel aria-labelledby="controls-title">
            <h2 id="controls-title" className="text-xl font-bold mb-5">Controls</h2>
            <div className="arcade-form-stack">
              <div className="arcade-actions">
                <ArcadeButton>Primary</ArcadeButton>
                <ArcadeButton variant="secondary">Secondary</ArcadeButton>
                <ArcadeButton variant="ghost">Ghost</ArcadeButton>
                <ArcadeButton variant="danger">Danger</ArcadeButton>
                <ArcadeButton variant="success">Success</ArcadeButton>
                <ArcadeButton disabled>Disabled</ArcadeButton>
                <ArcadeButton loading>Loading</ArcadeButton>
              </div>
              <div className="grid md:grid-cols-2 gap-4">
                <ArcadeInput label="Player name" defaultValue="PixelPilot" hint="Visible to the whole room." />
                <ArcadeSelect label="Rounds" defaultValue="3"><option>1</option><option>3</option><option>5</option></ArcadeSelect>
              </div>
              <div className="arcade-actions">
                <ArcadeBadge>Info</ArcadeBadge><ArcadeBadge tone="success">Ready</ArcadeBadge><ArcadeBadge tone="warning">Waiting</ArcadeBadge><ArcadeBadge tone="error">Offline</ArcadeBadge>
                <ConnectionBadge state="connected" /><ConnectionBadge state="reconnecting" />
              </div>
              <StatusBanner>Neutral table message.</StatusBanner>
              <StatusBanner tone="success">The room is ready to launch.</StatusBanner>
              <StatusBanner tone="error">The server rejected this action.</StatusBanner>
              <PlayerSeat name="LongNicknameThatNeedsTruncation" index={7} active meta="Current turn" badge={<ArcadeBadge tone="success">4 cards</ArcadeBadge>} />
              <ArcadeButton onClick={() => setDialogOpen(true)}>Open dialog</ArcadeButton>
            </div>
          </ArcadePanel>
        </div>
      )}

      <ArcadeDialog open={dialogOpen} title="Shared dialog" eyebrow="Focus test" onClose={() => setDialogOpen(false)} actions={<ArcadeButton onClick={() => setDialogOpen(false)}>Confirm</ArcadeButton>}>
        <ArcadeInput label="Invite code" defaultValue="ARCADE-8BIT" />
      </ArcadeDialog>
    </PageContainer>
  );
}
