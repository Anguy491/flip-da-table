import UnoCard from './UnoCard';

export default function DiscardPile({ top }) {
  if (!top) return <div className="uno-empty-pile">No discard</div>;
  return <UnoCard card={top} discard />;
}
