import { ArcadeShell } from './arcade/ArcadeUI';

function PageContainer({ children, theme = 'neutral', game = false, className = '' }) {
  return <ArcadeShell theme={theme} game={game} className={className}>{children}</ArcadeShell>;
}

export default PageContainer;
