import {
  ArcadeButton,
  ArcadePanel,
  ArcadeToolbar,
  ConnectionBadge,
  StatusBanner,
  ToolbarGroup,
} from '../arcade/ArcadeUI';
import { PlayerList } from './PlayerList';
import { MyHandPanel } from './MyHandPanel';
import { PendingCardBox } from './PendingCardBox';
import { InfoPanel } from './InfoPanel';
import { ControlPanel } from './ControlPanel';
import { DvcActionLog } from './DvcActionLog';

export default function DvcGameView({
  playerViews = [],
  myPlayerId,
  currentPlayerId,
  board,
  awaiting,
  roundIndex,
  myCards = [],
  pendingCard,
  publicTokens,
  canDragInitial,
  arrangementValid,
  isMyTurn,
  disabled,
  loading = false,
  error,
  connectionState = 'connected',
  selectedIndex,
  settledSubmitted,
  lastGuessCorrect,
  actionLog = [],
  onSelectSelf,
  onReorder,
  onOpponentCardClick,
  onBack,
  onRefresh,
  onDrawColor,
  onContinueReveal,
  onSelfReveal,
  onSettle,
}) {
  const blackRemaining = board?.deckBlackRemaining ?? 0;
  const whiteRemaining = board?.deckWhiteRemaining ?? 0;
  const isStartPhaseSettle = awaiting === 'SETTLE_POSITION' && !pendingCard;

  return (
    <div className="arcade-game-shell">
      <ArcadeToolbar className="arcade-toolbar--actions-only">
        <ToolbarGroup>
          <ConnectionBadge state={connectionState} />
          <ArcadeButton variant="ghost" size="small" onClick={onRefresh}>Sync</ArcadeButton>
          <ArcadeButton variant="ghost" size="small" onClick={onBack}>Leave table</ArcadeButton>
        </ToolbarGroup>
      </ArcadeToolbar>

      {loading && <StatusBanner>Syncing your private code view...</StatusBanner>}
      {error && <StatusBanner tone="error" live>{error}</StatusBanner>}
      {board?.winnerId && <StatusBanner tone="success">Winner: {board.winnerId}</StatusBanner>}

      <ArcadePanel padded={false} className="arcade-game-panel dvc-board">
        <section aria-labelledby="opponent-racks-title">
          <h2 id="opponent-racks-title" className="arcade-game-zone__title">Opponent code racks</h2>
          <PlayerList
            playerViews={playerViews}
            currentPlayerId={currentPlayerId}
            myPlayerId={myPlayerId}
            clickable={awaiting === 'GUESS_SELECTION' && isMyTurn}
            onOpponentCardClick={onOpponentCardClick}
          />
        </section>

        <div className="dvc-bottom-grid">
          <section className="dvc-rack" aria-labelledby="your-rack-title">
            <h2 id="your-rack-title" className="arcade-game-zone__title">
              YOUR CODE RACK <span className="arcade-text-muted">// {myPlayerId}</span>
            </h2>
            <div className="dvc-rack__hand">
              <MyHandPanel
                cards={myCards}
                draggable={canDragInitial && !disabled && !settledSubmitted}
                onReorder={onReorder}
                showValidity={awaiting === 'SETTLE_POSITION'}
                publicTokens={publicTokens}
                selectable={awaiting === 'SELF_REVEAL_CHOICE' && isMyTurn}
                selectedIndex={selectedIndex}
                onSelect={onSelectSelf}
              />
            </div>
            <DvcActionLog entries={actionLog} />
          </section>

          <PendingCardBox pending={pendingCard} />

          <section className="arcade-game-zone" aria-labelledby="control-console-title">
            <h2 id="control-console-title" className="arcade-game-zone__title">Control console</h2>
            <InfoPanel
              deckRemaining={board?.deckRemaining}
              deckBlackRemaining={blackRemaining}
              deckWhiteRemaining={whiteRemaining}
              currentPlayerId={currentPlayerId}
              roundIndex={roundIndex}
              awaiting={awaiting}
            />
            <div className="mt-4">
              <ControlPanel
                awaiting={awaiting}
                disabled={disabled}
                doDrawColor={onDrawColor}
                continueReveal={onContinueReveal}
                doSelfReveal={onSelfReveal}
                doSettle={onSettle}
                guessSucceeded={lastGuessCorrect}
                canSettle={isStartPhaseSettle ? arrangementValid : true}
                settledSubmitted={settledSubmitted}
                isStartPhaseSettle={isStartPhaseSettle}
                hasPending={Boolean(pendingCard)}
                isMyTurn={isMyTurn}
                selfRevealIndex={selectedIndex}
                blackRemaining={blackRemaining}
                whiteRemaining={whiteRemaining}
              />
            </div>
          </section>
        </div>
      </ArcadePanel>
    </div>
  );
}
