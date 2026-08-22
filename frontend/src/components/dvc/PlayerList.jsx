import { PlayerRow } from './PlayerRow';

export function PlayerList({ playerViews = [], currentPlayerId, myPlayerId, clickable = false, onOpponentCardClick }) {
  const opponents = playerViews.filter((player) => player.playerId !== myPlayerId);
  return (
    <div className="dvc-opponents">
      {opponents.map((player, index) => (
        <PlayerRow
          key={player.playerId}
          player={player}
          index={index}
          currentPlayerId={currentPlayerId}
          clickable={clickable}
          onCardClick={(cardIndex) => onOpponentCardClick?.(player.playerId, cardIndex)}
        />
      ))}
      {!opponents.length && <div className="arcade-empty">Waiting for opponent data...</div>}
    </div>
  );
}
