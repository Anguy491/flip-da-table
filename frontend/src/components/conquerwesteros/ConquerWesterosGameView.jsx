/* eslint-disable jsx-a11y/no-noninteractive-tabindex -- The horizontally scrollable seat rail needs a keyboard target. */
import { useEffect, useMemo, useState } from 'react';
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
  if (!isMyTurn) return `${currentPlayer?.name || 'Another player'} commands the next siege.`;
  if (view.phase === 'WAITING_FOR_ROLL') return view.attempt?.targetId
    ? 'Your siege continues. Roll the remaining dice when ready.'
    : 'Your turn. Roll all seven dice to scout the available strongholds.';
  if (view.phase === 'RESOLVING') return 'The capture is resolving through the event queue.';
  return 'Choose a stronghold, one unfinished battle line, and the dice committed to that line.';
}

function ClanMark({ clan, index = 0 }) {
  return (
    <span className={`cw-clan-mark cw-clan-mark--${index % 6}`} aria-hidden="true">
      <span>{clan?.slice(0, 1) || '?'}</span>
    </span>
  );
}

function StrongholdCard({ card, index, selected, selectable, memberCount, ownerName, onSelect }) {
  return (
    <button
      type="button"
      className={`cw-card ${selected ? 'cw-card--selected' : ''} ${card.locked ? 'cw-card--locked' : ''}`}
      aria-pressed={selected}
      disabled={!selectable}
      onClick={() => onSelect(card.id)}
    >
      <span className="cw-card__topline">
        <span className="arcade-label">{card.id}</span>
        <span className="cw-card__points">{card.locked ? memberCount : `${card.points} VP`}</span>
      </span>
      <ClanMark clan={card.clan} index={index} />
      {card.locked ? (
        <span className="cw-card__body">
          <strong>Clan secured</strong>
          <span>{card.clan}</span>
          <small>{memberCount} stronghold{memberCount === 1 ? '' : 's'} locked</small>
        </span>
      ) : (
        <span className="cw-card__body">
          <strong>{card.name}</strong>
          <span>{card.clan}</span>
          <small>{card.lines.map((line) => line.display).join(' / ')}</small>
        </span>
      )}
      <span className="cw-card__owner">
        {card.central ? 'Central field' : card.ownerId ? `Held by ${ownerName || card.ownerId}` : 'Unavailable'}
        {card.kingsLanding ? ' // Iron Throne' : ''}
      </span>
    </button>
  );
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
  const [rulesOpen, setRulesOpen] = useState(false);
  const players = view?.players || [];
  const strongholds = view?.strongholds || [];
  const currentPlayer = players.find((player) => player.playerId === view?.currentPlayerId);
  const isMyTurn = playerId === view?.currentPlayerId;
  const activeTargetId = view?.attempt?.targetId || selectedTargetId;
  const selectedCard = strongholds.find((card) => card.id === activeTargetId);
  const rollById = useMemo(() => new Map((view?.currentRoll || []).map((die) => [die.dieId, die])), [view?.currentRoll]);
  const committedIds = new Set(view?.attempt?.committedDieIds || []);
  const lostIds = new Set(view?.attempt?.lostDieIds || []);
  const clans = [...new Set(strongholds.map((card) => card.clan))];
  const selectedDice = selectedDieIds.map((id) => rollById.get(id)).filter(Boolean);

  let targetLines = view?.attempt?.targetId === activeTargetId
    ? view.attempt.requiredLines
    : selectedCard?.lines || [];
  if (selectedCard?.ownerId && selectedCard.ownerId !== playerId && !selectedCard.locked
    && !targetLines.some((line) => line.id === 'STEAL_CROWN')) {
    targetLines = [...targetLines, {
      id: 'STEAL_CROWN', type: 'STEAL_CROWN', symbols: ['CROWN'], display: 'Crown (steal)', completed: false, special: true,
    }];
  }
  const selectedLine = targetLines.find((line) => line.id === selectedLineId);
  const canSubmitLine = Boolean(
    view?.legalActions?.canCompleteLine
    && activeTargetId
    && selectedLine
    && !selectedLine.completed
    && lineMatches(selectedLine, selectedDice),
  );

  useEffect(() => {
    if (view?.attempt?.targetId) setSelectedTargetId(view.attempt.targetId);
    else if (view?.phase !== 'WAITING_FOR_DECISION') setSelectedTargetId(null);
  }, [view?.attempt?.targetId, view?.phase, view?.stateVersion]);

  useEffect(() => {
    setSelectedDieIds([]);
    setSelectedLineId(null);
  }, [view?.stateVersion]);

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
  const completedClanCount = (player) => player.completedClans?.length || 0;

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
          <ArcadeButton size="small" variant="ghost" onClick={() => setRulesOpen(true)}>Rules</ArcadeButton>
          <ArcadeButton size="small" variant="ghost" onClick={onLeave}>Exit</ArcadeButton>
        </ToolbarGroup>
      </ArcadeToolbar>

      <div className="cw-seat-rail" tabIndex="0" aria-label="Player seats">
        {players.map((player) => (
          <PlayerSeat
            key={player.playerId}
            name={player.name}
            index={player.seatIndex}
            active={player.current}
            meta={`${player.totalScore} VP // ${player.strongholdCount} holds // ${completedClanCount(player)} clans`}
            badge={player.holdsThrone ? <ArcadeBadge tone="warning">Throne</ArcadeBadge> : undefined}
          />
        ))}
      </div>

      <StatusBanner tone={view.phase === 'FINISHED' ? 'success' : isMyTurn ? 'warning' : 'info'} live>
        {phaseMessage(view, currentPlayer, isMyTurn)}
      </StatusBanner>
      {lastPublicEvent && <StatusBanner tone="success" live>{lastPublicEvent.text}</StatusBanner>}
      {error && <StatusBanner tone="error" live>{error}</StatusBanner>}
      {['reconnecting', 'offline'].includes(connectionState) && (
        <StatusBanner tone="warning" live>Live updates are interrupted. The table is polling for a safe recovery.</StatusBanner>
      )}

      <div className="cw-main-grid">
        <ArcadePanel className="cw-map" aria-labelledby="strongholds-title">
          <div className="cw-section-heading">
            <div>
              <p className="arcade-eyebrow">14 public strongholds</p>
              <h2 id="strongholds-title">Campaign map</h2>
            </div>
            <ArcadeBadge tone="muted">{strongholds.filter((card) => card.central).length} central</ArcadeBadge>
          </div>
          <div className="cw-card-grid">
            {strongholds.map((card) => {
              const owner = players.find((player) => player.playerId === card.ownerId);
              const clan = owner?.completedClans?.find((item) => item.name === card.clan);
              const legalTarget = view.legalActions?.legalTargetIds?.includes(card.id);
              const frozenElsewhere = Boolean(view.attempt?.targetId && view.attempt.targetId !== card.id);
              return (
                <StrongholdCard
                  key={card.id}
                  card={card}
                  index={clans.indexOf(card.clan)}
                  memberCount={clan?.strongholdIds?.length || 1}
                  ownerName={owner?.name}
                  selected={activeTargetId === card.id}
                  selectable={Boolean(isMyTurn && view.phase === 'WAITING_FOR_DECISION' && legalTarget && !frozenElsewhere)}
                  onSelect={(id) => { setSelectedTargetId(id); setSelectedLineId(null); setSelectedDieIds([]); }}
                />
              );
            })}
          </div>
        </ArcadePanel>

        <aside className="cw-side-stack">
          <ArcadePanel quiet>
            <p className="arcade-eyebrow">Iron Throne // +1 VP</p>
            <div className="cw-throne">
              <span className="cw-throne__icon" aria-hidden="true"><span /></span>
              <strong>{players.find((player) => player.playerId === view.ironThroneHolderId)?.name || 'Unclaimed'}</strong>
            </div>
            <p className="arcade-copy text-sm">Capture King's Landing, or steal any face-up stronghold from the current holder.</p>
          </ArcadePanel>

          <ArcadePanel quiet aria-labelledby="siege-title">
            <p className="arcade-eyebrow">Current operation</p>
            <h2 id="siege-title">{selectedCard ? `Siege: ${selectedCard.name}` : 'Choose a target'}</h2>
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
            ) : <p className="arcade-copy">A target locks after you complete its first line.</p>}
          </ArcadePanel>

          <ArcadePanel quiet aria-labelledby="events-title">
            <p className="arcade-eyebrow">Last 200 events</p>
            <h2 id="events-title">Campaign log</h2>
            <ol className="cw-log">
              {[...(view.events || [])].reverse().slice(0, 12).map((event) => (
                <li key={event.sequence}><span>#{event.sequence}</span>{event.text}</li>
              ))}
            </ol>
          </ArcadePanel>
        </aside>
      </div>

      <ArcadePanel className="cw-console" aria-labelledby="dice-console-title">
        <div className="cw-section-heading">
          <div>
            <p className="arcade-eyebrow">Stable dice // D1-D7</p>
            <h2 id="dice-console-title">Siege console</h2>
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
      </ArcadePanel>

      {view.phase === 'FINISHED' && (
        <ArcadePanel aria-labelledby="ranking-title">
          <p className="arcade-eyebrow">Campaign complete</p>
          <h2 id="ranking-title">Final ranking</h2>
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
          <ArcadeButton className="mt-5" onClick={onSummary}>Open session summary</ArcadeButton>
        </ArcadePanel>
      )}

      <ArcadeDialog
        open={rulesOpen}
        wide
        title="Conquer Westeros rules"
        eyebrow="MVP quick reference"
        onClose={() => setRulesOpen(false)}
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
