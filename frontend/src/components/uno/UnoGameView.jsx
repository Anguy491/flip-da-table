import DiscardPile from './DiscardPile';
import PlayerArea from './layout/PlayerArea';
import InfoPanel from './layout/InfoPanel';
import EventLog from './layout/EventLog';
import HandArea from './layout/HandArea';
import {
  ArcadeButton,
  ArcadePanel,
  ArcadeToolbar,
  ConnectionBadge,
  StatusBanner,
  ToolbarGroup,
} from '../arcade/ArcadeUI';

export default function UnoGameView({
  players = [],
  currentPlayerId,
  round = 1,
  direction = 'CW',
  activeColor,
  pendingDraw = 0,
  topCard,
  events = [],
  hand = [],
  playableCards = [],
  myTurn = false,
  mustChooseColor = false,
  finished = false,
  sending = false,
  loading = false,
  error,
  connectionState = 'connected',
  onBack,
  onPlay,
  onDraw,
  onOpenColorPicker,
}) {
  const playableIds = new Set(playableCards.map((card) => card.id || card));
  const currentPlayerName = players.find((player) => player.id === currentPlayerId)?.name;
  const instruction = loading
    ? 'Syncing the table...'
    : finished
      ? 'Round complete. Saving the score...'
      : myTurn
        ? mustChooseColor
          ? 'Choose the next active color.'
          : playableCards.length
            ? 'Your turn: play a highlighted card or draw.'
            : 'No playable card. Draw from the deck.'
        : `Waiting for ${currentPlayerName || 'the next player'}...`;

  return (
    <div className="arcade-game-shell">
      <ArcadeToolbar className="arcade-toolbar--actions-only">
        <ToolbarGroup>
          <ConnectionBadge state={connectionState} />
          <ArcadeButton variant="ghost" size="small" onClick={onBack}>Leave table</ArcadeButton>
        </ToolbarGroup>
      </ArcadeToolbar>

      {error && <StatusBanner tone="error" live>{error}</StatusBanner>}

      <ArcadePanel padded={false} className="arcade-game-panel">
        <section aria-labelledby="opponents-title">
          <h2 id="opponents-title" className="sr-only">Players</h2>
          <PlayerArea players={players} currentPlayerId={currentPlayerId} />
        </section>

        <div className="arcade-game-grid-main mt-2">
          <section className="arcade-game-zone flex flex-col items-center justify-center" aria-labelledby="discard-title">
            <h2 id="discard-title" className="arcade-game-zone__title">Discard</h2>
            <DiscardPile top={topCard} />
          </section>
          <section className="arcade-game-zone" aria-labelledby="round-title">
            <h2 id="round-title" className="arcade-game-zone__title">Round status</h2>
            <InfoPanel gameCount={round} direction={direction} activeColor={activeColor} currentPlayerName={currentPlayerName} pendingDraw={pendingDraw} />
          </section>
          <section className="arcade-game-zone uno-feed--desktop" aria-labelledby="event-title">
            <h2 id="event-title" className="arcade-game-zone__title">Live feed</h2>
            <EventLog events={events} />
          </section>
          <details className="arcade-game-zone uno-feed--mobile">
            <summary className="arcade-game-zone__title">Live feed</summary>
            <EventLog events={events} />
          </details>
        </div>

        <div className="arcade-instruction my-3" aria-live="polite">
          <p>{instruction}</p>
          {myTurn && mustChooseColor && (
            <ArcadeButton className="mt-3" onClick={onOpenColorPicker}>Choose color</ArcadeButton>
          )}
        </div>

        <section className="arcade-hand-zone" aria-labelledby="hand-title">
          <h2 id="hand-title" className="arcade-game-zone__title">Your hand // {hand.length} cards</h2>
          <HandArea
            hand={hand}
            playableIds={playableIds}
            disabled={!myTurn || mustChooseColor || finished || loading}
            sending={sending}
            onPlay={onPlay}
            onDraw={onDraw}
            pendingDraw={pendingDraw}
          />
        </section>
      </ArcadePanel>
    </div>
  );
}
