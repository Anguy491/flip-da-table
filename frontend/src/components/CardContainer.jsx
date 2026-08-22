import { ArcadePanel } from './arcade/ArcadeUI';

function CardContainer({ children, className = '', noMax = false }) {
  const maxClass = noMax ? 'max-w-none' : 'max-w-lg';
  return (
    <ArcadePanel className={`w-full ${maxClass} ${className}`}>
      {children}
    </ArcadePanel>
  );
}

export default CardContainer;
