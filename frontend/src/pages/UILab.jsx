import { useState } from 'react';
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
  const [dialogOpen, setDialogOpen] = useState(false);
  const [colorPickerOpen, setColorPickerOpen] = useState(wildMode);
  const theme = screen === 'uno' ? 'uno' : screen === 'dvc' ? 'dvc' : screen === 'vegas' ? 'vegas' : 'neutral';

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
          view={lasVegasFixture}
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
          onRoll={() => {}}
          onPlace={() => {}}
          onSkip={() => {}}
          onToggleAssets={() => {}}
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
