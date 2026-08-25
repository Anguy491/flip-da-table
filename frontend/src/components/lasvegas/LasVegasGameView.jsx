/* eslint-disable jsx-a11y/no-noninteractive-tabindex -- The button-free scrollable seat rail needs a keyboard target. */
import { useState } from 'react';
import {
  ArcadeBadge,
  ArcadeButton,
  ArcadeDialog,
  ArcadePanel,
  ArcadeToolbar,
  ConnectionBadge,
  PlayerSeat,
  Scoreboard,
  StatusBanner,
  ToolbarGroup,
} from '../arcade/ArcadeUI';

function money(amount) {
  return amount == null ? 'Hidden' : `$${Number(amount).toLocaleString('en-US')}`;
}

function Die({ face, big = false, seatIndex = 0, count, label, showOwner = true }) {
  return (
    <span
      className={`vegas-die ${big ? 'vegas-die--big' : ''}`}
      data-seat={seatIndex + 1}
      role="img"
      aria-label={label || `${big ? 'Big die' : 'Die'} showing ${face}`}
    >
      <span className="vegas-die__face" aria-hidden="true">{face}</span>
      {showOwner && <span className="vegas-die__owner" aria-hidden="true">P{seatIndex + 1}{big ? ' ×2' : count ? ` ×${count}` : ''}</span>}
    </span>
  );
}

function phaseMessage(view, isMyTurn, currentPlayer) {
  if (view.phase === 'FINISHED') return 'The final casino has settled. All assets are now public.';
  if (view.phase === 'RESOLVING') return 'The house is resolving ties and paying the six casinos.';
  if (!isMyTurn) return `${currentPlayer?.name || 'Another player'}${currentPlayer?.bot ? ' (CPU)' : ''} is taking their turn.`;
  if (view.phase === 'WAITING_FOR_ROLL') return 'Your turn: roll every remaining die.';
  return 'Choose one rolled face and place every matching die, or spend one chip to skip.';
}

export default function LasVegasGameView({
  gameId,
  view,
  playerId,
  connectionState,
  loading,
  sending,
  error,
  publicEvents = [],
  assetsVisible,
  onRoll,
  onPlace,
  onSkip,
  onToggleAssets,
  onRefresh,
  onLeave,
  onSummary,
}) {
  const [selectedCasinoNumber, setSelectedCasinoNumber] = useState(null);
  const players = view?.players || [];
  const me = players.find((player) => player.playerId === playerId);
  const currentPlayer = players.find((player) => player.playerId === view?.currentPlayerId);
  const isMyTurn = playerId === view?.currentPlayerId;
  const legalFaces = [...new Set((view?.currentRoll || []).map((die) => die.face))].sort((left, right) => left - right);
  const settlementEvents = publicEvents.filter((event) => ['CASINO_JACKPOT', 'CASINO_SECOND_PRIZE'].includes(event.type));
  const highlightedCasino = settlementEvents.at(-1)?.casinoNumber;
  const selectedCasino = view?.casinos.find((casino) => casino.number === selectedCasinoNumber);

  if (!view) {
    return (
      <ArcadePanel className="max-w-2xl mx-auto text-center">
        <p className="arcade-eyebrow">Casino link // {loading ? 'syncing' : 'unavailable'}</p>
        <h1 className="arcade-title">{loading ? 'Loading the table' : 'No Las Vegas game found'}</h1>
        {error && <StatusBanner tone="error" className="mt-5">{error}</StatusBanner>}
        <div className="arcade-actions mt-6 justify-center">
          <ArcadeButton variant="ghost" onClick={onLeave}>Back to lobby</ArcadeButton>
          <ArcadeButton loading={loading} onClick={onRefresh}>Try sync</ArcadeButton>
        </div>
      </ArcadePanel>
    );
  }

  const resultColumns = [
    { key: 'rank', label: 'Rank', render: (row) => row.rank === 1 ? `#${row.rank} WIN` : `#${row.rank}` },
    { key: 'name', label: 'Player' },
    { key: 'cashTotal', label: 'Cash', render: (row) => money(row.cashTotal) },
    { key: 'chips', label: 'Chips' },
    { key: 'tieBreakCount', label: 'Cards + chips' },
    { key: 'totalAssets', label: 'Total', render: (row) => money(row.totalAssets) },
  ];

  return (
    <div className="arcade-game-shell vegas-game">
      <ArcadeToolbar className="arcade-toolbar--actions-only">
        <ToolbarGroup>
          <ConnectionBadge state={connectionState} />
          <ArcadeButton size="small" variant="ghost" loading={loading} onClick={onRefresh}>Sync</ArcadeButton>
          <ArcadeButton size="small" variant="ghost" onClick={onLeave}>Leave table</ArcadeButton>
        </ToolbarGroup>
      </ArcadeToolbar>

      <StatusBanner live tone={view.phase === 'FINISHED' ? 'success' : isMyTurn ? 'warning' : 'info'}>
        {phaseMessage(view, isMyTurn, currentPlayer)}
      </StatusBanner>
      {error && <StatusBanner tone="error" live>{error}</StatusBanner>}

      {settlementEvents.length > 0 && (
        <div className="vegas-settlement" role="status" aria-live="polite">
          {settlementEvents.map((event) => (
            <span key={`${event.sequence}-${event.type}-${event.actorId}`}>
              Casino {event.casinoNumber}: {players.find((player) => player.playerId === event.actorId)?.name || event.actorId} won {money(event.amount)}
            </span>
          ))}
        </div>
      )}

      <div className="vegas-table-layout">
        <section className="vegas-seat-track" aria-label="Player seats" tabIndex={0}>
          {players.map((player) => (
            <PlayerSeat
              key={player.playerId}
              className="vegas-seat"
              data-seat={player.seatIndex + 1}
              name={player.name}
              index={player.seatIndex}
              active={player.current}
              meta={`${player.remainingDice} dice // ${player.chips} chips`}
              badge={player.playerId === playerId
                ? <ArcadeBadge tone="success">You</ArcadeBadge>
                : player.bot ? <ArcadeBadge tone="muted">CPU</ArcadeBadge> : undefined}
            >
              <span className="vegas-seat__assets">
                {player.totalAssets != null ? `Mine ${money(player.totalAssets)}` : player.presentedTotal != null ? `Revealed ${money(player.presentedTotal)}` : 'Assets hidden'}
              </span>
            </PlayerSeat>
          ))}
        </section>

        <section className="vegas-casino-grid" aria-label="Six casinos">
          {view.casinos.map((casino) => {
            const previewPlacements = casino.placements.length > 2
              ? casino.placements.slice(0, 1)
              : casino.placements;
            const hiddenPlacementCount = casino.placements.length - previewPlacements.length;
            const openCasino = () => setSelectedCasinoNumber(casino.number);

            return (
              <ArcadePanel
                as="article"
                key={casino.number}
                padded={false}
                className={`vegas-casino ${highlightedCasino === casino.number ? 'vegas-casino--settling' : ''}`}
                role="button"
                tabIndex={0}
                aria-haspopup="dialog"
                aria-expanded={selectedCasinoNumber === casino.number}
                aria-label={`Open Casino ${casino.number} details, ${casino.placements.length} players`}
                onClick={openCasino}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter' && event.key !== ' ') return;
                  event.preventDefault();
                  openCasino();
                }}
              >
                <header className="vegas-casino__header">
                  <div>
                    <p className="arcade-eyebrow">Casino</p>
                    <h2 className="vegas-casino__number">{casino.number}</h2>
                  </div>
                  <div className="vegas-casino__bonuses" aria-label="Public prizes">
                    {casino.bonuses.map((bonus, index) => (
                      <span key={`${bonus}-${index}`} className="vegas-money-card">{index === 0 ? '1ST' : '2ND'} {money(bonus)}</span>
                    ))}
                  </div>
                </header>
                <div className="vegas-casino__players">
                  {previewPlacements.length ? previewPlacements.map((placement) => {
                    const player = players.find((candidate) => candidate.playerId === placement.playerId);
                    return (
                      <div className="vegas-influence" data-seat={(player?.seatIndex || 0) + 1} key={placement.playerId}>
                        <span className="vegas-influence__name">P{(player?.seatIndex || 0) + 1} {player?.name}</span>
                        <span className="vegas-influence__dice">
                          {placement.regularDice > 0 && <Die face={casino.number} seatIndex={player?.seatIndex || 0} count={placement.regularDice} showOwner={false} label={`${player?.name} has ${placement.regularDice} regular dice at casino ${casino.number}`} />}
                          {placement.bigDie && <Die face={casino.number} big seatIndex={player?.seatIndex || 0} showOwner={false} label={`${player?.name} has a big die worth two at casino ${casino.number}`} />}
                        </span>
                        <ArcadeBadge tone="muted">Power {placement.influence}</ArcadeBadge>
                      </div>
                    );
                  }) : <p className="vegas-casino__empty">No dice placed</p>}
                  {hiddenPlacementCount > 0 && (
                    <p className="vegas-casino__more">+{hiddenPlacementCount} more players // open details</p>
                  )}
                </div>
              </ArcadePanel>
            );
          })}
        </section>

        <aside className="vegas-side-column" aria-label="Table controls and event logs">
          <ArcadePanel className="vegas-console" aria-labelledby="vegas-actions-title">
            <div className="vegas-console__header">
              <div>
                <p className="arcade-eyebrow">Action console</p>
                <h2 id="vegas-actions-title" className="text-xl font-bold">{isMyTurn ? 'Your move' : 'Table locked'}</h2>
              </div>
              {view.phase !== 'FINISHED' && (
                <ArcadeButton size="small" variant={assetsVisible ? 'secondary' : 'ghost'} loading={sending} onClick={() => onToggleAssets(!assetsVisible)}>
                  {assetsVisible ? 'Hide total assets' : 'Reveal total assets'}
                </ArcadeButton>
              )}
            </div>

            {view.phase === 'WAITING_FOR_ROLL' && (
              <ArcadeButton block className="mt-5" loading={sending} disabled={!isMyTurn} onClick={onRoll}>Roll {me?.remainingDice || 0} dice</ArcadeButton>
            )}

            {view.phase === 'WAITING_FOR_CHOICE' && (
              <>
                <div className="vegas-current-roll" aria-label="Current public roll">
                  {view.currentRoll.map((die, index) => <Die key={`${die.face}-${die.big}-${index}`} {...die} seatIndex={currentPlayer?.seatIndex || 0} />)}
                </div>
                <div className="vegas-face-actions" aria-label="Legal casino choices">
                  {legalFaces.map((face) => (
                    <ArcadeButton key={face} loading={sending} disabled={!isMyTurn} onClick={() => onPlace(face)}>Place all {face}s</ArcadeButton>
                  ))}
                </div>
                <ArcadeButton block variant="secondary" className="mt-3" loading={sending} disabled={!isMyTurn || !me?.chips} onClick={onSkip}>
                  Spend 1 chip to skip
                </ArcadeButton>
              </>
            )}

            {view.phase === 'FINISHED' && <ArcadeButton block className="mt-5" onClick={onSummary}>Open final scoreboard</ArcadeButton>}
          </ArcadePanel>

          <details className="vegas-event-panel" open>
            <summary>Event logs ({view.events.length})</summary>
            <div role="log" aria-label="Las Vegas event record">
              <ol className="vegas-event-log">
                {[...view.events].reverse().map((event) => (
                  <li key={event.sequence}>
                    <span className="arcade-code">#{event.sequence}</span> {event.text}
                  </li>
                ))}
              </ol>
            </div>
          </details>
        </aside>
      </div>

      {view.phase === 'FINISHED' && (
        <ArcadePanel aria-labelledby="vegas-final-title">
          <p className="arcade-eyebrow">Session complete // all assets revealed</p>
          <h2 id="vegas-final-title" className="text-xl font-bold mb-5">Final high scores</h2>
          <Scoreboard columns={resultColumns} rows={view.results} getRowKey={(row) => row.playerId} />
        </ArcadePanel>
      )}

      <ArcadeDialog
        open={Boolean(selectedCasino)}
        wide
        title={selectedCasino ? `Casino ${selectedCasino.number} details` : 'Casino details'}
        eyebrow={selectedCasino ? `${selectedCasino.placements.length} players // all dice and influence` : undefined}
        closeLabel="Close"
        onClose={() => setSelectedCasinoNumber(null)}
      >
        {selectedCasino && (
          <div className="vegas-casino-detail">
            {selectedCasino.placements.length ? selectedCasino.placements.map((placement) => {
              const player = players.find((candidate) => candidate.playerId === placement.playerId);
              const playerName = player?.name || placement.playerId;
              const seatIndex = player?.seatIndex || 0;
              return (
                <section className="vegas-casino-detail__player" data-seat={seatIndex + 1} key={placement.playerId}>
                  <header className="vegas-casino-detail__header">
                    <h3>P{seatIndex + 1} {playerName}</h3>
                    <ArcadeBadge tone="muted">Power {placement.influence}</ArcadeBadge>
                  </header>
                  <div className="vegas-casino-detail__dice" aria-label={`${playerName}'s dice at casino ${selectedCasino.number}`}>
                    {Array.from({ length: placement.regularDice }, (_, index) => (
                      <Die
                        key={`regular-${index}`}
                        face={selectedCasino.number}
                        seatIndex={seatIndex}
                        showOwner={false}
                        label={`${playerName} regular die ${index + 1} at casino ${selectedCasino.number}`}
                      />
                    ))}
                    {placement.bigDie && (
                      <Die
                        big
                        face={selectedCasino.number}
                        seatIndex={seatIndex}
                        showOwner={false}
                        label={`${playerName} big die worth two at casino ${selectedCasino.number}`}
                      />
                    )}
                  </div>
                </section>
              );
            }) : <p className="vegas-casino-detail__empty">No player has placed dice at this casino.</p>}
          </div>
        )}
      </ArcadeDialog>

      <span className="sr-only">Game ID {gameId}</span>
    </div>
  );
}
