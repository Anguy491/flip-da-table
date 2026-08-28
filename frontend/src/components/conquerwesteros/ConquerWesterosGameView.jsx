import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArcadeBadge,
  ArcadeButton,
  ArcadeDialog,
  ArcadePanel,
  ArcadeToolbar,
  ConnectionBadge,
  Scoreboard,
  StatusBanner,
  ToolbarGroup,
} from '../arcade/ArcadeUI';
import WesterosCampaignMap from './WesterosCampaignMap';

const DIE_IDS = [0, 1, 2, 3, 4, 5, 6];

function faceGlyph(face) {
  return {
    MILITARY_1: 'I',
    MILITARY_2: 'II',
    MILITARY_3: 'III',
    RAVEN: 'R',
    KNIGHT: 'K',
    CROWN: 'C',
  }[face] || '?';
}

function lineMatches(line, selectedDice) {
  if (!line || !selectedDice.length) return false;
  if (line.type === 'MILITARY') {
    return selectedDice.every((die) => die.militaryStrength > 0)
      && selectedDice.reduce((total, die) => total + die.militaryStrength, 0) >= line.threshold;
  }
  const actual = selectedDice.map((die) => die.face).sort();
  const required = [...(line.symbols || [])].sort();
  return actual.length === required.length && actual.every((face, index) => face === required[index]);
}

function phaseMessage(view, currentPlayer, isMyTurn) {
  if (view.phase === 'FINISHED') return 'The campaign is complete. Tied players share a rank only after every tie-break is equal.';
  if (!isMyTurn && currentPlayer?.bot) {
    if (view.phase === 'WAITING_FOR_ROLL') return `${currentPlayer.name} (CPU) is preparing the next roll.`;
    if (view.phase === 'WAITING_FOR_DECISION') return `${currentPlayer.name} (CPU) is evaluating the public war table.`;
    return `${currentPlayer.name} (CPU) is resolving the siege.`;
  }
  if (!isMyTurn) return `${currentPlayer?.name || 'Another player'} commands the next siege.`;
  if (view.phase === 'WAITING_FOR_ROLL') return view.attempt?.targetId
    ? 'Your siege continues. Roll the remaining dice when ready.'
    : 'Your turn. Roll all seven dice to scout the available strongholds.';
  if (view.phase === 'RESOLVING') return 'The capture is resolving through the event queue.';
  return 'Choose a stronghold, one unfinished battle line, and the dice committed to that line.';
}

function linesForTarget(view, card, playerId) {
  if (!card) return [];
  let lines = view?.attempt?.targetId === card.id
    ? view.attempt.requiredLines
    : card.lines || [];
  if (card.ownerId && card.ownerId !== playerId && !card.locked
    && !lines.some((line) => line.id === 'STEAL_CROWN')) {
    lines = [...lines, {
      id: 'STEAL_CROWN', type: 'STEAL_CROWN', symbols: ['CROWN'], display: 'Crown (steal)', completed: false, special: true,
    }];
  }
  return lines;
}

function captureAvailability(view, card, isMyTurn, activeTargetId) {
  if (!card) return { selectable: false, reason: 'Stronghold data is unavailable.' };
  if (view.phase === 'FINISHED') return { selectable: false, reason: 'The campaign is complete.' };
  if (!isMyTurn) return { selectable: false, reason: 'Only the current player can choose a siege target.' };
  if (view.phase !== 'WAITING_FOR_DECISION') {
    return { selectable: false, reason: 'Targets can be chosen after the siege dice are rolled.' };
  }
  if (card.locked) return { selectable: false, reason: 'This clan is secured and cannot be targeted.' };
  if (view.attempt?.targetId && view.attempt.targetId !== card.id) {
    return { selectable: false, reason: 'The current siege is already locked to another stronghold.' };
  }
  if (activeTargetId === card.id) {
    return { selectable: false, reason: view.attempt?.targetId ? 'This is the current locked target.' : 'This target is already selected.' };
  }
  if (!view.legalActions?.legalTargetIds?.includes(card.id)) {
    return { selectable: false, reason: 'This stronghold is not a legal target right now.' };
  }
  return { selectable: true, reason: 'Review the battle lines, then confirm this siege target.' };
}

function DieButton({ id, die, committed, lost, selected, disabled, onToggle }) {
  const label = lost ? 'Lost' : committed ? 'Committed' : die ? die.display : 'Ready';
  return (
    <button
      type="button"
      className={`cw-die ${selected ? 'cw-die--selected' : ''} ${lost ? 'cw-die--lost' : ''} ${committed ? 'cw-die--committed' : ''}`}
      aria-pressed={selected}
      aria-label={`Die ${id + 1}: ${label}`}
      disabled={disabled}
      onClick={() => onToggle(id)}
    >
      <span className="cw-die__id">D{id + 1}</span>
      <strong>{die ? faceGlyph(die.face) : lost ? 'X' : committed ? '✓' : '·'}</strong>
      <span>{label}</span>
    </button>
  );
}

function PlayerAvatarButton({ player, onClick }) {
  return (
    <button
      type="button"
      className={`cw-player-avatar ${player.current ? 'cw-player-avatar--current' : ''}`}
      aria-label={`Open player details for ${player.name}.${player.bot ? ' CPU.' : ''}${player.current ? ' Current turn.' : ''}${player.holdsThrone ? ' Holds the Iron Throne.' : ''}`}
      aria-current={player.current ? 'true' : undefined}
      title={player.name}
      onClick={onClick}
    >
      <span className="cw-player-avatar__code">P{player.seatIndex + 1}</span>
      {player.current && <span className="cw-player-avatar__marker cw-player-avatar__marker--turn" aria-hidden="true">T</span>}
      {player.bot && <span className="cw-player-avatar__marker cw-player-avatar__marker--cpu" aria-hidden="true">CPU</span>}
      {player.holdsThrone && <span className="cw-player-avatar__marker cw-player-avatar__marker--throne" aria-hidden="true">K</span>}
    </button>
  );
}

function MapDockButton({ label, shortLabel, icon, onClick }) {
  return (
    <button
      type="button"
      className={`cw-dock-button cw-dock-button--${icon}`}
      aria-label={label}
      title={label}
      onClick={onClick}
    >
      {icon === 'throne' ? (
        <span className="cw-throne__icon cw-throne__icon--compact" aria-hidden="true"><span /></span>
      ) : (
        <span className="cw-dock-button__glyph" aria-hidden="true">
          {icon === 'tips' ? '?' : icon === 'log' ? 'III' : '#1'}
        </span>
      )}
      <span className="cw-dock-button__label">{shortLabel}</span>
    </button>
  );
}

function SummaryStat({ label, children }) {
  return (
    <div>
      <span className="arcade-label">{label}</span>
      <strong>{children}</strong>
    </div>
  );
}

export default function ConquerWesterosGameView({
  view,
  playerId,
  loading,
  sending,
  error,
  connectionState,
  publicEvents = [],
  onRoll,
  onCompleteLine,
  onLoseDie,
  onRefresh,
  onLeave,
  onSummary,
}) {
  const [selectedTargetId, setSelectedTargetId] = useState(null);
  const [selectedLineId, setSelectedLineId] = useState(null);
  const [selectedDieIds, setSelectedDieIds] = useState([]);
  const [activeDialog, setActiveDialog] = useState(null);
  const confirmTargetRef = useRef(null);
  const previousCurrentPlayerRef = useRef(view?.currentPlayerId);
  const players = view?.players || [];
  const strongholds = view?.strongholds || [];
  const currentPlayer = players.find((player) => player.playerId === view?.currentPlayerId);
  const isMyTurn = playerId === view?.currentPlayerId;
  const activeTargetId = view?.attempt?.targetId || selectedTargetId;
  const selectedCard = strongholds.find((card) => card.id === activeTargetId);
  const inspectedStrongholdId = activeDialog?.type === 'stronghold' ? activeDialog.id : null;
  const inspectedCard = strongholds.find((card) => card.id === inspectedStrongholdId);
  const inspectedPlayer = activeDialog?.type === 'player'
    ? players.find((player) => player.playerId === activeDialog.id)
    : null;
  const rollById = useMemo(() => new Map((view?.currentRoll || []).map((die) => [die.dieId, die])), [view?.currentRoll]);
  const committedIds = new Set(view?.attempt?.committedDieIds || []);
  const lostIds = new Set(view?.attempt?.lostDieIds || []);
  const availableDiceCount = DIE_IDS.filter((id) => !committedIds.has(id) && !lostIds.has(id)).length;
  const selectedDice = selectedDieIds.map((id) => rollById.get(id)).filter(Boolean);

  const targetLines = linesForTarget(view, selectedCard, playerId);
  const inspectedLines = linesForTarget(view, inspectedCard, playerId);
  const selectedLine = targetLines.find((line) => line.id === selectedLineId);
  const canSubmitLine = Boolean(
    view?.legalActions?.canCompleteLine
    && activeTargetId
    && selectedLine
    && !selectedLine.completed
    && lineMatches(selectedLine, selectedDice),
  );
  const inspectedOwner = players.find((player) => player.playerId === inspectedCard?.ownerId);
  const inspectedClan = inspectedOwner?.completedClans?.find((item) => item.name === inspectedCard?.clan);
  const captureOption = captureAvailability(view, inspectedCard, isMyTurn, activeTargetId);

  useEffect(() => {
    if (view?.attempt?.targetId) setSelectedTargetId(view.attempt.targetId);
    else if (view?.phase !== 'WAITING_FOR_DECISION') setSelectedTargetId(null);
  }, [view?.attempt?.targetId, view?.phase, view?.stateVersion]);

  useEffect(() => {
    setSelectedDieIds([]);
    setSelectedLineId(null);
  }, [view?.stateVersion]);

  useEffect(() => {
    const previousPlayerId = previousCurrentPlayerRef.current;
    if (previousPlayerId && previousPlayerId !== view?.currentPlayerId) {
      setActiveDialog((current) => current?.type === 'siege' ? null : current);
    }
    previousCurrentPlayerRef.current = view?.currentPlayerId;
  }, [view?.currentPlayerId]);

  useEffect(() => {
    if (view?.phase === 'FINISHED') {
      setActiveDialog({ type: 'results' });
    }
  }, [view?.phase]);

  const selectInspectedTarget = () => {
    if (!captureOption.selectable || !inspectedCard) return;
    setSelectedTargetId(inspectedCard.id);
    setSelectedLineId(null);
    setSelectedDieIds([]);
    setActiveDialog(null);
  };

  if (loading && !view) {
    return <ArcadePanel className="cw-loading"><p className="arcade-eyebrow">Loading campaign</p><h1 className="arcade-title">Raising the banners...</h1></ArcadePanel>;
  }

  if (!view) {
    return (
      <ArcadePanel className="cw-loading">
        <p className="arcade-eyebrow">War table unavailable</p>
        <h1 className="arcade-title">The ravens lost their way</h1>
        <StatusBanner tone="error">{error || 'No campaign state was returned.'}</StatusBanner>
        <ArcadeButton onClick={onRefresh}>Retry</ArcadeButton>
      </ArcadePanel>
    );
  }

  const lastPublicEvent = publicEvents.at(-1);
  const throneHolder = players.find((player) => player.playerId === view.ironThroneHolderId);
  const events = [...(view.events || [])].reverse();
  const inspectedPlayerStrongholds = inspectedPlayer?.faceUpStrongholds
    ?.map((id) => strongholds.find((card) => card.id === id))
    .filter(Boolean) || [];

  return (
    <div className="cw-table">
      <ArcadeToolbar>
        <ToolbarGroup>
          <div>
            <p className="arcade-eyebrow">Conquer Westeros // {view.campaignName}</p>
            <h1 className="arcade-title">War table</h1>
          </div>
          <ArcadeBadge tone="muted">Turn {view.turnCount + 1}</ArcadeBadge>
        </ToolbarGroup>
        <ToolbarGroup>
          <ConnectionBadge state={connectionState} />
          <ArcadeButton size="small" variant="ghost" onClick={() => setActiveDialog({ type: 'rules' })}>Rules</ArcadeButton>
          <ArcadeButton size="small" variant="ghost" onClick={onLeave}>Exit</ArcadeButton>
        </ToolbarGroup>
      </ArcadeToolbar>

      <ArcadePanel padded={false} className="cw-map-shell" aria-labelledby="strongholds-title">
        <div className="cw-section-heading cw-map-shell__heading">
          <div>
            <p className="arcade-eyebrow">{strongholds.length} public strongholds</p>
            <h2 id="strongholds-title">Campaign map</h2>
          </div>
          <ArcadeBadge tone="muted">{strongholds.filter((card) => card.central).length} central</ArcadeBadge>
        </div>

        {(error || ['reconnecting', 'offline'].includes(connectionState) || lastPublicEvent) && (
          <div className="cw-map-alerts">
            {error && <StatusBanner tone="error" live>{error}</StatusBanner>}
            {['reconnecting', 'offline'].includes(connectionState) && (
              <StatusBanner tone="warning" live>Live updates are interrupted. The table is polling for a safe recovery.</StatusBanner>
            )}
            {lastPublicEvent && <StatusBanner tone="success" live>{lastPublicEvent.text}</StatusBanner>}
          </div>
        )}

        <div className="cw-map-layout">
          <nav className="cw-player-dock" aria-label="Players">
            {players.map((player) => (
              <PlayerAvatarButton
                key={player.playerId}
                player={player}
                onClick={() => setActiveDialog({ type: 'player', id: player.playerId })}
              />
            ))}
          </nav>

          <WesterosCampaignMap
            campaign={view.campaign}
            strongholds={strongholds}
            players={players}
            activeTargetId={activeTargetId}
            legalTargetIds={isMyTurn && view.phase === 'WAITING_FOR_DECISION'
              ? view.legalActions?.legalTargetIds
              : []}
            onInspect={(id) => setActiveDialog({ type: 'stronghold', id })}
          />

          <nav className="cw-utility-dock" aria-label="Game details">
            <MapDockButton
              label="Open Iron Throne details"
              shortLabel="Throne"
              icon="throne"
              onClick={() => setActiveDialog({ type: 'throne' })}
            />
            <MapDockButton
              label="Open current operation tips"
              shortLabel="Tips"
              icon="tips"
              onClick={() => setActiveDialog({ type: 'tips' })}
            />
            <MapDockButton
              label="Open campaign log"
              shortLabel="Log"
              icon="log"
              onClick={() => setActiveDialog({ type: 'log' })}
            />
            {view.phase === 'FINISHED' && (
              <MapDockButton
                label="Open final ranking"
                shortLabel="Results"
                icon="results"
                onClick={() => setActiveDialog({ type: 'results' })}
              />
            )}
          </nav>

          <div className="cw-roll-entry">
            <ArcadeButton
              className="cw-roll-launcher"
              variant={isMyTurn ? 'primary' : 'ghost'}
              disabled={view.phase === 'FINISHED'}
              aria-haspopup="dialog"
              aria-label={view.phase === 'FINISHED' ? 'Campaign complete' : 'Roll Dice'}
              onClick={() => setActiveDialog({ type: 'siege' })}
            >
              <span>{view.phase === 'FINISHED' ? 'Campaign complete' : 'Roll Dice'}</span>
              {view.phase !== 'FINISHED' && <ArcadeBadge tone={isMyTurn ? 'warning' : 'muted'}>{availableDiceCount}/7</ArcadeBadge>}
            </ArcadeButton>
          </div>
        </div>
      </ArcadePanel>

      <ArcadeDialog
        open={Boolean(inspectedPlayer)}
        title={inspectedPlayer?.name || 'Player details'}
        eyebrow={inspectedPlayer ? `P${inspectedPlayer.seatIndex + 1} // ${inspectedPlayer.bot ? 'CPU' : 'Player'}` : undefined}
        onClose={() => setActiveDialog(null)}
        className="cw-detail-dialog"
      >
        {inspectedPlayer && (
          <div className="cw-detail-stack">
            <div className="cw-player-dialog__identity">
              <span className={`cw-player-avatar cw-player-avatar--static ${inspectedPlayer.current ? 'cw-player-avatar--current' : ''}`} aria-hidden="true">
                <span className="cw-player-avatar__code">P{inspectedPlayer.seatIndex + 1}</span>
              </span>
              <div>
                <strong>{inspectedPlayer.name}</strong>
                <p>{inspectedPlayer.current ? 'Current commander' : 'Waiting for their turn'}{inspectedPlayer.holdsThrone ? ' // Holds the Iron Throne' : ''}</p>
              </div>
            </div>
            <div className="cw-detail-summary">
              <SummaryStat label="Total score">{inspectedPlayer.totalScore} VP</SummaryStat>
              <SummaryStat label="Face-up score">{inspectedPlayer.faceUpScore} VP</SummaryStat>
              <SummaryStat label="Clan score">{inspectedPlayer.clanScore} VP</SummaryStat>
              <SummaryStat label="Strongholds">{inspectedPlayer.strongholdCount}</SummaryStat>
            </div>
            <section>
              <p className="arcade-eyebrow">Controlled strongholds</p>
              {inspectedPlayerStrongholds.length ? (
                <ul className="cw-detail-list">
                  {inspectedPlayerStrongholds.map((card) => <li key={card.id}><span>{card.id}</span><strong>{card.name}</strong><small>{card.points} VP</small></li>)}
                </ul>
              ) : <p className="arcade-copy">No face-up strongholds.</p>}
            </section>
            <section>
              <p className="arcade-eyebrow">Completed clans</p>
              {inspectedPlayer.completedClans?.length ? (
                <ul className="cw-detail-list">
                  {inspectedPlayer.completedClans.map((clan) => <li key={clan.name}><span>{clan.strongholdIds.length}</span><strong>{clan.name}</strong><small>{clan.score} VP</small></li>)}
                </ul>
              ) : <p className="arcade-copy">No completed clans.</p>}
            </section>
          </div>
        )}
      </ArcadeDialog>

      <ArcadeDialog
        open={activeDialog?.type === 'throne'}
        title="Iron Throne"
        eyebrow="Campaign bonus // +1 VP"
        onClose={() => setActiveDialog(null)}
        className="cw-detail-dialog"
      >
        <div className="cw-detail-stack">
          <div className="cw-throne cw-throne--dialog">
            <span className="cw-throne__icon" aria-hidden="true"><span /></span>
            <div>
              <span className="arcade-label">Current holder</span>
              <strong>{throneHolder?.name || 'Unclaimed'}</strong>
            </div>
          </div>
          <StatusBanner tone="warning">The Iron Throne contributes 1 VP and is the first final-ranking tie-break.</StatusBanner>
          <p className="arcade-copy">Capture King's Landing, or steal any face-up stronghold from the current holder, to claim the throne.</p>
        </div>
      </ArcadeDialog>

      <ArcadeDialog
        open={activeDialog?.type === 'tips'}
        title="Operation tips"
        eyebrow={`Turn ${view.turnCount + 1} // ${view.phase.replaceAll('_', ' ')}`}
        onClose={() => setActiveDialog(null)}
        className="cw-detail-dialog"
      >
        <div className="cw-detail-stack">
          <StatusBanner tone={view.phase === 'FINISHED' ? 'success' : isMyTurn ? 'warning' : 'info'}>
            {phaseMessage(view, currentPlayer, isMyTurn)}
          </StatusBanner>
          <div className="cw-detail-summary">
            <SummaryStat label="Commander">{currentPlayer?.name || 'Unavailable'}</SummaryStat>
            <SummaryStat label="Target">{selectedCard?.name || 'Not selected'}</SummaryStat>
            <SummaryStat label="Connection">{connectionState || 'Unknown'}</SummaryStat>
            <SummaryStat label="Dice ready">{availableDiceCount}/7</SummaryStat>
          </div>
          {selectedCard ? (
            <section>
              <p className="arcade-eyebrow">Current operation // {selectedCard.id}</p>
              <ol className="cw-operation-lines">
                {targetLines.map((line) => (
                  <li key={line.id} className={line.completed ? 'cw-operation-lines__completed' : ''}>
                    <span>{line.special ? 'STEAL' : line.id}</span>
                    <strong>{line.display}</strong>
                    <small>{line.completed ? 'Completed' : 'Open'}</small>
                  </li>
                ))}
              </ol>
            </section>
          ) : <p className="arcade-copy">Inspect a legal stronghold on the map and set it as the siege target.</p>}
          {error && <StatusBanner tone="error">{error}</StatusBanner>}
          {lastPublicEvent && <StatusBanner tone="success">Latest: {lastPublicEvent.text}</StatusBanner>}
        </div>
      </ArcadeDialog>

      <ArcadeDialog
        open={activeDialog?.type === 'log'}
        title="Campaign log"
        eyebrow={`Last ${events.length} of 200 events`}
        onClose={() => setActiveDialog(null)}
        className="cw-log-dialog"
      >
        {events.length ? (
          <ol className="cw-log cw-log--dialog" role="log" aria-label="Campaign log">
            {events.map((event) => (
              <li key={event.sequence}><span>#{event.sequence}</span>{event.text}</li>
            ))}
          </ol>
        ) : <p className="arcade-copy">No campaign events have been recorded yet.</p>}
      </ArcadeDialog>

      <ArcadeDialog
        open={activeDialog?.type === 'siege'}
        wide
        title="Siege console"
        eyebrow="Stable dice // D1-D7"
        onClose={() => setActiveDialog(null)}
        className="cw-siege-dialog"
      >
        <div className="cw-detail-stack">
          <StatusBanner tone={view.phase === 'WAITING_FOR_DECISION' ? 'warning' : isMyTurn ? 'info' : 'warning'}>
            {phaseMessage(view, currentPlayer, isMyTurn)}
          </StatusBanner>
          <div className="cw-siege-dialog__layout">
            <section className="cw-siege-dialog__operation" aria-labelledby="siege-operation-title">
              <p className="arcade-eyebrow">Current operation</p>
              <h3 id="siege-operation-title">{selectedCard ? `Siege: ${selectedCard.name}` : 'Choose a target'}</h3>
              {selectedCard ? (
                <div className="cw-lines" role="group" aria-label="Battle lines">
                  {targetLines.map((line) => (
                    <button
                      key={line.id}
                      type="button"
                      className={`cw-line ${selectedLineId === line.id ? 'cw-line--selected' : ''}`}
                      aria-pressed={selectedLineId === line.id}
                      disabled={line.completed || view.phase !== 'WAITING_FOR_DECISION' || !isMyTurn}
                      onClick={() => { setSelectedLineId(line.id); setSelectedDieIds([]); }}
                    >
                      <span>{line.special ? 'STEAL' : line.id}</span>
                      <strong>{line.display}</strong>
                      <small>{line.completed ? 'Completed' : 'Open'}</small>
                    </button>
                  ))}
                </div>
              ) : <p className="arcade-copy">Close the console and select a stronghold from the map after rolling.</p>}
            </section>
            <section className="cw-siege-dialog__dice" aria-labelledby="siege-dice-title">
              <div className="cw-section-heading">
                <div>
                  <p className="arcade-eyebrow">Remaining // {availableDiceCount} of 7</p>
                  <h3 id="siege-dice-title">Siege dice</h3>
                </div>
                <ArcadeBadge tone={view.phase === 'WAITING_FOR_DECISION' ? 'warning' : 'muted'}>{view.phase.replaceAll('_', ' ')}</ArcadeBadge>
              </div>
              <div className="cw-dice" role="group" aria-label="Seven siege dice">
                {DIE_IDS.map((id) => (
                  <DieButton
                    key={id}
                    id={id}
                    die={rollById.get(id)}
                    committed={committedIds.has(id)}
                    lost={lostIds.has(id)}
                    selected={selectedDieIds.includes(id)}
                    disabled={!rollById.has(id) || !isMyTurn || view.phase !== 'WAITING_FOR_DECISION'}
                    onToggle={(dieId) => setSelectedDieIds((current) => current.includes(dieId)
                      ? current.filter((item) => item !== dieId)
                      : [...current, dieId])}
                  />
                ))}
              </div>
            </section>
          </div>
          <div className="cw-console__actions">
            <ArcadeButton
              loading={sending}
              disabled={!view.legalActions?.canRoll || sending}
              onClick={onRoll}
            >Roll remaining dice</ArcadeButton>
            <ArcadeButton
              variant="success"
              loading={sending}
              disabled={!canSubmitLine || sending}
              onClick={() => onCompleteLine(activeTargetId, selectedLineId, selectedDieIds)}
            >Complete line</ArcadeButton>
            <ArcadeButton
              variant="danger"
              loading={sending}
              disabled={!view.legalActions?.canLoseDie || selectedDieIds.length !== 1 || sending}
              onClick={() => onLoseDie(selectedDieIds[0])}
            >Lose selected die</ArcadeButton>
          </div>
          <p className="arcade-copy text-sm">Each roll can complete at most one line. A lost die and all committed dice stay unavailable for the rest of this turn.</p>
        </div>
      </ArcadeDialog>

      <ArcadeDialog
        open={activeDialog?.type === 'results'}
        wide
        title="Final ranking"
        eyebrow="Campaign complete"
        onClose={() => setActiveDialog(null)}
        actions={<ArcadeButton onClick={onSummary}>Open session summary</ArcadeButton>}
      >
        <Scoreboard
          columns={[
            { key: 'rank', label: 'Rank', render: (row) => `#${row.rank}${row.winner ? ' WIN' : ''}` },
            { key: 'name', label: 'Player' },
            { key: 'totalScore', label: 'VP' },
            { key: 'thronePoint', label: 'Throne' },
            { key: 'strongholdCount', label: 'Holds' },
            { key: 'completedClanCount', label: 'Clans' },
          ]}
          rows={view.results || []}
          getRowKey={(row) => row.playerId}
        />
      </ArcadeDialog>

      <ArcadeDialog
        open={Boolean(inspectedCard)}
        title={inspectedCard?.name || 'Stronghold details'}
        eyebrow={inspectedCard ? `${inspectedCard.id} // ${inspectedCard.clan}` : undefined}
        onClose={() => setActiveDialog(null)}
        className="cw-stronghold-dialog"
        initialFocusRef={captureOption.selectable ? confirmTargetRef : undefined}
        actions={inspectedCard && (
          <ArcadeButton
            ref={confirmTargetRef}
            disabled={!captureOption.selectable}
            onClick={selectInspectedTarget}
          >
            {activeTargetId === inspectedCard.id ? 'Current target' : 'Set as target'}
          </ArcadeButton>
        )}
      >
        {inspectedCard && (
          <div className="cw-stronghold-details">
            <div className="cw-stronghold-details__summary">
              <div>
                <span className="arcade-label">Victory points</span>
                <strong>{inspectedCard.points} VP</strong>
              </div>
              <div>
                <span className="arcade-label">Control</span>
                <strong>
                  {inspectedCard.locked
                    ? 'Clan secured'
                    : inspectedOwner
                      ? `Held by ${inspectedOwner.name}`
                      : inspectedCard.central
                        ? 'Open field'
                        : 'Unavailable'}
                </strong>
              </div>
            </div>

            {inspectedCard.locked && (
              <StatusBanner tone="success">
                {inspectedCard.clan} is secured with {inspectedClan?.strongholdIds?.length || 1} locked stronghold{(inspectedClan?.strongholdIds?.length || 1) === 1 ? '' : 's'}.
              </StatusBanner>
            )}
            {inspectedCard.kingsLanding && (
              <StatusBanner tone="warning">Capturing this stronghold claims the Iron Throne and its 1 VP bonus.</StatusBanner>
            )}

            <div>
              <p className="arcade-eyebrow">Capture requirements</p>
              <ol className="cw-stronghold-details__lines">
                {inspectedLines.map((line) => (
                  <li key={line.id} className={line.completed ? 'cw-stronghold-details__line--completed' : ''}>
                    <span>{line.special ? 'STEAL' : line.id}</span>
                    <strong>{line.display}</strong>
                    <small>{line.completed ? 'COMPLETED' : line.special ? 'EXTRA LINE' : 'OPEN'}</small>
                  </li>
                ))}
              </ol>
            </div>

            <StatusBanner tone={captureOption.selectable ? 'info' : 'warning'}>
              {captureOption.reason}
            </StatusBanner>
          </div>
        )}
      </ArcadeDialog>

      <ArcadeDialog
        open={activeDialog?.type === 'rules'}
        wide
        title="Conquer Westeros rules"
        eyebrow="MVP quick reference"
        onClose={() => setActiveDialog(null)}
      >
        <div className="cw-rules arcade-copy">
          <p>On your turn, click Roll. From that result, commit dice to exactly one battle line or lose one die. If the siege continues, click Roll again.</p>
          <p>Military lines accept only military dice whose total meets or exceeds the target. Symbol lines require the exact shown symbols. A die can serve only one line.</p>
          <p>Your first completed line locks the target. Stealing a face-up stronghold adds a separate Crown line; a printed Crown never also completes that line.</p>
          <p>Completing a clan flips and locks all of its strongholds. The clan score replaces their individual scores. The Iron Throne is worth 1 VP.</p>
          <p>Final ties break by Iron Throne, stronghold count, then completed clan count. If all remain equal, the players share the rank.</p>
        </div>
      </ArcadeDialog>
    </div>
  );
}
