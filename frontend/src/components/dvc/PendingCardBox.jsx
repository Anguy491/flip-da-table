import { ArcadeBadge } from '../arcade/ArcadeUI';
import { CardTile } from './CardTile';
import { parseCard } from './parseCard';

export function PendingCardBox({ pending }) {
  return (
    <section className="dvc-pending" aria-labelledby="pending-card-title">
      <div className="text-center">
        <h3 id="pending-card-title" className="arcade-game-zone__title">Pending tile</h3>
        <ArcadeBadge tone={pending ? 'warning' : 'muted'}>{pending ? 'Unsettled' : 'Empty'}</ArcadeBadge>
      </div>
      {pending ? <CardTile card={parseCard(pending)} /> : <span className="arcade-muted text-xs">Draw to reveal a tile.</span>}
    </section>
  );
}
