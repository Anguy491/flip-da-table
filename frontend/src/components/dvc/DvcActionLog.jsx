import { useCallback, useEffect, useRef, useState } from 'react';
import { ArcadeBadge } from '../arcade/ArcadeUI';

export function DvcActionLog({ entries = [] }) {
  const logRef = useRef(null);
  const [autoScroll, setAutoScroll] = useState(true);

  const handleScroll = useCallback(() => {
    const element = logRef.current;
    if (!element) return;
    setAutoScroll(Math.abs(element.scrollHeight - element.clientHeight - element.scrollTop) < 4);
  }, []);

  useEffect(() => {
    const element = logRef.current;
    if (!element) return undefined;
    element.addEventListener('scroll', handleScroll);
    return () => element.removeEventListener('scroll', handleScroll);
  }, [handleScroll]);

  useEffect(() => {
    const element = logRef.current;
    if (element && autoScroll) element.scrollTop = element.scrollHeight;
  }, [entries, autoScroll]);

  return (
    <div className="dvc-log-region">
      <div className="dvc-log-region__header">
        <h3 id="dvc-game-log-title" className="arcade-game-zone__title mb-0">Game log</h3>
        <ArcadeBadge tone="muted">{entries.length} events</ArcadeBadge>
      </div>
      <div
        ref={logRef}
        className="dvc-action-log"
        role="log"
        aria-labelledby="dvc-game-log-title"
        aria-live="polite"
        aria-relevant="additions"
      >
        {entries.map((entry, index) => (
          <div
            key={entry.seq ?? `${entry.text}-${index}`}
            className="dvc-action-log__entry"
            data-result={entry.correct == null ? 'neutral' : entry.correct ? 'correct' : 'wrong'}
            data-type={entry.type || 'EVENT'}
          >
            <span className="dvc-action-log__seq" aria-hidden="true">
              #{String(entry.seq ?? index + 1).padStart(2, '0')}
            </span>
            <span>{entry.text}</span>
          </div>
        ))}
        {!entries.length && <div className="dvc-action-log__empty">No moves recorded yet.</div>}
      </div>
    </div>
  );
}
