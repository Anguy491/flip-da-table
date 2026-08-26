/* eslint-disable jsx-a11y/no-noninteractive-tabindex -- The button-free scrollable seat rail needs a keyboard target. */
import { useCallback, useEffect, useRef, useState } from 'react';
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
import LasVegasDie from './LasVegasDie';
import RollRevealDialog from './RollRevealDialog';

function money(amount) {
  return amount == null ? 'Hidden' : `$${Number(amount).toLocaleString('en-US')}`;
}

function pendingDiceFor(player) {
  const regularDice = Number.isInteger(player?.remainingRegularDice)
    ? player.remainingRegularDice
    : Math.max(0, (player?.remainingDice || 0) - (player?.bigDieRemaining ? 1 : 0));
  return [
    ...Array.from({ length: regularDice }, () => ({ face: null, big: false })),
    ...(player?.bigDieRemaining ? [{ face: null, big: true }] : []),
  ];
}

function diceSignature(dice = []) {
  return dice.map((die) => `${die.face}:${Boolean(die.big)}`).join('|');
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
  const [rollReveal, setRollReveal] = useState(null);
  const rollSequenceRef = useRef(0);
  const showRollButtonRef = useRef(null);
  const actionConsoleRef = useRef(null);
  const focusShowAfterHideRef = useRef(false);
  const focusConsoleAfterActionRef = useRef(false);
  const players = view?.players || [];
  const me = players.find((player) => player.playerId === playerId);
  const currentPlayer = players.find((player) => player.playerId === view?.currentPlayerId);
  const isMyTurn = playerId === view?.currentPlayerId;
  const legalFaces = [...new Set((view?.currentRoll || []).map((die) => die.face))].sort((left, right) => left - right);
  const settlementEvents = publicEvents.filter((event) => ['CASINO_JACKPOT', 'CASINO_SECOND_PRIZE'].includes(event.type));
  const highlightedCasino = settlementEvents.at(-1)?.casinoNumber;
  const selectedCasino = view?.casinos.find((casino) => casino.number === selectedCasinoNumber);

  const startRollReveal = useCallback(() => {
    if (!view || !me || !isMyTurn || sending || view.phase !== 'WAITING_FOR_ROLL') return;
    rollSequenceRef.current += 1;
    setRollReveal({
      id: rollSequenceRef.current,
      actorId: view.currentPlayerId,
      playerName: me.name,
      seatIndex: me.seatIndex || 0,
      pendingDice: pendingDiceFor(me),
      resultDice: null,
      resultVersion: null,
      phase: 'waiting-result',
      visible: true,
      pendingAction: null,
      submittedVersion: null,
      requestStarted: false,
      actionError: '',
      errorAtOpen: error,
    });
    onRoll?.();
  }, [error, isMyTurn, me, onRoll, sending, view]);

  useEffect(() => {
    if (!rollReveal || rollReveal.resultDice?.length) return;
    if (view?.phase === 'WAITING_FOR_CHOICE'
      && view.currentPlayerId === rollReveal.actorId
      && view.currentRoll?.length) {
      setRollReveal((current) => current ? {
        ...current,
        resultDice: view.currentRoll.map((die) => ({ ...die })),
        resultVersion: view.stateVersion,
        phase: 'revealing',
      } : current);
      return;
    }
    const rollStillPending = view?.phase === 'WAITING_FOR_ROLL'
      && view.currentPlayerId === rollReveal.actorId;
    if (!rollStillPending) setRollReveal(null);
  }, [rollReveal, view?.currentPlayerId, view?.currentRoll, view?.phase, view?.stateVersion]);

  useEffect(() => {
    if (!rollReveal?.resultDice?.length) return;
    const sameChoice = view?.phase === 'WAITING_FOR_CHOICE'
      && view.currentPlayerId === rollReveal.actorId
      && view.currentRoll?.length;
    const baselineVersion = rollReveal.pendingAction
      ? rollReveal.submittedVersion
      : rollReveal.resultVersion;
    const versionAdvanced = baselineVersion == null
      || view?.stateVersion == null
      || Number(view.stateVersion) > Number(baselineVersion);
    if (!sameChoice) {
      if (!versionAdvanced) return;
      if (rollReveal.pendingAction) focusConsoleAfterActionRef.current = true;
      setRollReveal(null);
      return;
    }
    if (versionAdvanced && diceSignature(view.currentRoll) !== diceSignature(rollReveal.resultDice)) {
      setRollReveal((current) => current ? {
        ...current,
        resultDice: view.currentRoll.map((die) => ({ ...die })),
        resultVersion: view.stateVersion,
        phase: 'ready',
        pendingAction: null,
        submittedVersion: null,
        requestStarted: false,
      } : current);
    }
  }, [rollReveal, view?.currentPlayerId, view?.currentRoll, view?.phase, view?.stateVersion]);

  useEffect(() => {
    if (!rollReveal) return;
    if (!rollReveal.resultDice?.length && error && error !== rollReveal.errorAtOpen) {
      setRollReveal(null);
      return;
    }
    if (rollReveal.resultDice?.length
      && rollReveal.pendingAction
      && error
      && error !== rollReveal.errorAtSubmit) {
      setRollReveal((current) => current ? {
        ...current,
        pendingAction: null,
        submittedVersion: null,
        requestStarted: false,
        actionError: error,
        errorAtSubmit: error,
      } : current);
    }
  }, [error, rollReveal]);

  useEffect(() => {
    if (!rollReveal?.pendingAction && !rollReveal?.resultDice?.length) {
      if (rollReveal && sending && !rollReveal.requestStarted) {
        setRollReveal((current) => current ? { ...current, requestStarted: true } : current);
      } else if (rollReveal?.requestStarted && !sending && view?.phase === 'WAITING_FOR_ROLL') {
        setRollReveal(null);
      }
      return;
    }
    if (!rollReveal?.pendingAction) return;
    if (sending && !rollReveal.requestStarted) {
      setRollReveal((current) => current ? { ...current, requestStarted: true } : current);
    } else if (!sending && rollReveal.requestStarted
      && view?.phase === 'WAITING_FOR_CHOICE'
      && view.currentPlayerId === rollReveal.actorId) {
      setRollReveal((current) => current ? {
        ...current,
        pendingAction: null,
        submittedVersion: null,
        requestStarted: false,
        actionError: error || 'The casino did not advance. Please try again.',
      } : current);
    }
  }, [error, rollReveal, sending, view?.currentPlayerId, view?.phase]);

  useEffect(() => {
    if (rollReveal?.visible || !focusShowAfterHideRef.current) return undefined;
    focusShowAfterHideRef.current = false;
    const frame = window.requestAnimationFrame(() => showRollButtonRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [rollReveal?.visible]);

  useEffect(() => {
    if (rollReveal || !focusConsoleAfterActionRef.current) return undefined;
    focusConsoleAfterActionRef.current = false;
    const frame = window.requestAnimationFrame(() => actionConsoleRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [rollReveal]);

  const finishRollReveal = useCallback(() => {
    setRollReveal((current) => current ? { ...current, phase: 'ready' } : current);
  }, []);

  const hideRollReveal = useCallback(() => {
    focusShowAfterHideRef.current = true;
    setRollReveal((current) => current?.phase === 'ready' ? { ...current, visible: false } : current);
  }, []);

  const showRollReveal = useCallback(() => {
    if (!view || !me || !isMyTurn || view.phase !== 'WAITING_FOR_CHOICE' || !view.currentRoll?.length) return;
    setRollReveal((current) => {
      if (current) return { ...current, visible: true };
      rollSequenceRef.current += 1;
      return {
        id: rollSequenceRef.current,
        actorId: view.currentPlayerId,
        playerName: me.name,
        seatIndex: me.seatIndex || 0,
        pendingDice: view.currentRoll.map((die) => ({ ...die })),
        resultDice: view.currentRoll.map((die) => ({ ...die })),
        resultVersion: view.stateVersion,
        phase: 'ready',
        visible: true,
        pendingAction: null,
        submittedVersion: null,
        requestStarted: false,
        actionError: '',
        errorAtOpen: error,
      };
    });
  }, [error, isMyTurn, me, view]);

  const placeFromReveal = useCallback((face) => {
    if (!rollReveal || rollReveal.phase !== 'ready' || rollReveal.pendingAction || sending) return;
    setRollReveal((current) => current ? {
      ...current,
      pendingAction: `place-${face}`,
      submittedVersion: view?.stateVersion,
      requestStarted: false,
      actionError: '',
      errorAtSubmit: error,
    } : current);
    onPlace?.(face);
  }, [error, onPlace, rollReveal, sending, view?.stateVersion]);

  const skipFromReveal = useCallback(() => {
    if (!rollReveal || rollReveal.phase !== 'ready' || rollReveal.pendingAction || sending || !me?.chips) return;
    setRollReveal((current) => current ? {
      ...current,
      pendingAction: 'skip',
      submittedVersion: view?.stateVersion,
      requestStarted: false,
      actionError: '',
      errorAtSubmit: error,
    } : current);
    onSkip?.();
  }, [error, me?.chips, onSkip, rollReveal, sending, view?.stateVersion]);

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
                          {placement.regularDice > 0 && <LasVegasDie face={casino.number} seatIndex={player?.seatIndex || 0} count={placement.regularDice} showOwner={false} label={`${player?.name} has ${placement.regularDice} regular dice at casino ${casino.number}`} />}
                          {placement.bigDie && <LasVegasDie face={casino.number} big seatIndex={player?.seatIndex || 0} showOwner={false} label={`${player?.name} has a big die worth two at casino ${casino.number}`} />}
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
                <h2 ref={actionConsoleRef} id="vegas-actions-title" className="text-xl font-bold" tabIndex={-1}>{isMyTurn ? 'Your move' : 'Table locked'}</h2>
              </div>
              {view.phase !== 'FINISHED' && (
                <ArcadeButton size="small" variant={assetsVisible ? 'secondary' : 'ghost'} loading={sending} onClick={() => onToggleAssets(!assetsVisible)}>
                  {assetsVisible ? 'Hide total assets' : 'Reveal total assets'}
                </ArcadeButton>
              )}
            </div>

            {view.phase === 'WAITING_FOR_ROLL' && (
              <ArcadeButton id="vegas-roll-button" block className="mt-5" loading={sending} disabled={!isMyTurn} onClick={startRollReveal}>Roll {me?.remainingDice || 0} dice</ArcadeButton>
            )}

            {view.phase === 'WAITING_FOR_CHOICE' && isMyTurn && (!rollReveal || !rollReveal.visible) && (
              <ArcadeButton
                ref={showRollButtonRef}
                block
                className="mt-5"
                onClick={showRollReveal}
              >
                Show roll &amp; actions
              </ArcadeButton>
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
                      <LasVegasDie
                        key={`regular-${index}`}
                        face={selectedCasino.number}
                        seatIndex={seatIndex}
                        showOwner={false}
                        label={`${playerName} regular die ${index + 1} at casino ${selectedCasino.number}`}
                      />
                    ))}
                    {placement.bigDie && (
                      <LasVegasDie
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

      {rollReveal && (
        <RollRevealDialog
          open={rollReveal.visible}
          rollId={rollReveal.id}
          phase={rollReveal.phase}
          pendingDice={rollReveal.pendingDice}
          resultDice={rollReveal.resultDice}
          seatIndex={rollReveal.seatIndex}
          playerName={rollReveal.playerName}
          legalFaces={legalFaces}
          chips={me?.chips || 0}
          sending={sending}
          pendingAction={rollReveal.pendingAction}
          error={rollReveal.actionError}
          onRevealComplete={finishRollReveal}
          onHide={hideRollReveal}
          onPlace={placeFromReveal}
          onSkip={skipFromReveal}
        />
      )}

      <span className="sr-only">Game ID {gameId}</span>
    </div>
  );
}
