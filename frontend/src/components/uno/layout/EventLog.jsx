import { useEffect, useRef, useState, useCallback } from 'react';

export default function EventLog({ events = [] }) {
  const ref = useRef(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const handleScroll = useCallback(() => {
    const element = ref.current;
    if (!element) return;
    setAutoScroll(Math.abs(element.scrollHeight - element.clientHeight - element.scrollTop) < 4);
  }, []);

  useEffect(() => {
    const element = ref.current;
    if (!element) return undefined;
    element.addEventListener('scroll', handleScroll);
    return () => element.removeEventListener('scroll', handleScroll);
  }, [handleScroll]);

  useEffect(() => {
    const element = ref.current;
    if (element && autoScroll) element.scrollTop = element.scrollHeight;
  }, [events, autoScroll]);

  return (
    <div ref={ref} className="arcade-log" aria-label="Game event log" aria-live="polite">
      {events.map((event, index) => <div key={event.id || `${event.text}-${index}`} className="arcade-log__entry">{event.text}</div>)}
      {!events.length && <div className="arcade-muted">No events yet.</div>}
    </div>
  );
}
