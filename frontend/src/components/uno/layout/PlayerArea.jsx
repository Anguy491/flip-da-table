import { ArcadeBadge, PlayerSeat } from '../../arcade/ArcadeUI';

export default function PlayerArea({ players = [], currentPlayerId }) {
  return (
    // Keyboard focus makes the horizontally scrollable ten-player rail reachable in Safari.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
    <div className="arcade-seat-strip" role="region" aria-label="Players" tabIndex={0}>
      {players.map((player, index) => (
        <PlayerSeat
          key={player.id}
          name={player.name || player.id}
          index={index}
          active={player.id === currentPlayerId}
          meta={player.id === currentPlayerId ? 'Current turn' : 'Waiting'}
          badge={<ArcadeBadge tone={player.id === currentPlayerId ? 'success' : 'muted'}>{player.handCount ?? 0} cards</ArcadeBadge>}
        />
      ))}
      {!players.length && <div className="arcade-muted text-sm">Waiting for player data...</div>}
    </div>
  );
}
