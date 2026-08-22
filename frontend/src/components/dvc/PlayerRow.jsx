import { ArcadeBadge } from '../arcade/ArcadeUI';
import { CardStrip } from './CardStrip';
import { parseCard } from './parseCard';

export function PlayerRow({ player, index, currentPlayerId, clickable = false, onCardClick }) {
  const active = player.playerId === currentPlayerId;
  return (
    <article className={`dvc-player-row ${active ? 'dvc-player-row--active' : ''}`} data-testid={`player-${index}`}>
      <span className="dvc-player-row__avatar" aria-hidden="true">P{index + 1}</span>
      <div className="min-w-0">
        <div className="flex items-center gap-2 mb-2">
          <strong className="arcade-code text-xs" title={player.playerId}>{player.playerId}</strong>
          {active && <ArcadeBadge tone="success">Current</ArcadeBadge>}
        </div>
        <div className="overflow-x-auto">
          <CardStrip cards={(player.cards || []).map(parseCard)} clickable={clickable} onCardClick={onCardClick} />
        </div>
      </div>
      <ArcadeBadge tone={player.hiddenCount > 0 ? 'muted' : 'success'}>{player.hiddenCount > 0 ? `${player.hiddenCount} hidden` : 'Revealed'}</ArcadeBadge>
    </article>
  );
}
