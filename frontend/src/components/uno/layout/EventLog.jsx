import React, { useEffect, useRef, useState, useCallback } from 'react';

/** EventLog with auto-scroll to bottom unless user scrolled up. */
export default function EventLog({ events = [] }) {
  const ref = useRef(null);
  const [autoScroll, setAutoScroll] = useState(true);

  const handleScroll = useCallback(() => {
    const el = ref.current; if (!el) return;
    const atBottom = Math.abs(el.scrollHeight - el.clientHeight - el.scrollTop) < 4;
    setAutoScroll(atBottom);
  }, []);

  useEffect(() => { const el = ref.current; if (!el) return; el.addEventListener('scroll', handleScroll); return () => el.removeEventListener('scroll', handleScroll); }, [handleScroll]);
  useEffect(() => {
    const el = ref.current; if (!el) return;
    if (autoScroll) { el.scrollTop = el.scrollHeight; }
  }, [events, autoScroll]);

  return (
    <div ref={ref} className="w-full h-full overflow-auto p-2 text-xs space-y-1 bg-base-200/40 rounded">
      {events.map(ev => (
        <div key={ev.id} className="leading-snug">{ev.text}</div>
      ))}
      {events.length === 0 && <div className="opacity-50">No events</div>}
    </div>
  );
}
