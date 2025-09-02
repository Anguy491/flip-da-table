import { useEffect, useRef } from 'react';

// view: latest UNO view object (must contain events[] and optional lastEventSeq)
// gameId: reset key when a new game instance starts so we replay animations
export default function usePlayAnimations({ view, gameId }) {
  const lastSeqRef = useRef(0);
  const queueRef = useRef([]);
  const playingRef = useRef(false);
  const styleInjectedRef = useRef(false);
  const gameRef = useRef();
  const processedDrawRef = useRef(new Set());

  const DURATION = 900;
  const GAP = 120;

  useEffect(() => {
    if (styleInjectedRef.current) return;
  const css = `@keyframes unoPlayPulse {0%{transform:translate(-50%,-50%) scale(1)}30%{transform:translate(-50%,-50%) scale(1.38) rotate(-4deg)}45%{transform:translate(-50%,-50%) scale(1.30) rotate(3deg)}60%{transform:translate(-50%,-50%) scale(1.36) rotate(-2deg)}75%{transform:translate(-50%,-50%) scale(1.26) rotate(2deg)}100%{transform:translate(-50%,-50%) scale(1)} }
    @keyframes unoRing {0%{transform:translate(-50%,-50%) scale(.55);opacity:.85;}65%{opacity:.25;}100%{transform:translate(-50%,-50%) scale(1.5);opacity:0;}}
    .uno-play-overlay{position:fixed;left:0;top:0;z-index:9998;pointer-events:none;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:18px;width:80px;height:120px;border-radius:12px;border:2px solid #fff;box-shadow:0 4px 12px rgba(0,0,0,.35);transform:translate(-50%,-50%) scale(1);}
    .uno-play-overlay.animating{animation:unoPlayPulse ${DURATION}ms cubic-bezier(.34,1.56,.4,1);}
    .uno-play-overlay .ring{position:absolute;left:50%;top:50%;width:100%;height:100%;border:3px solid rgba(255,255,255,.55);border-radius:14px;transform:translate(-50%,-50%) scale(.55);opacity:.9;animation:unoRing ${DURATION}ms ease forwards;}
  .uno-draw-flash{transition:all .15s ease;font-weight:600 !important;}
  .uno-draw-flash-active{background:#2563eb !important;color:#fff !important;box-shadow:0 0 0 2px rgba(37,99,235,.4);scale:1.15;}
    `;
    const tag = document.createElement('style');
    tag.id = 'uno-play-overlay-style';
    tag.textContent = css;
    document.head.appendChild(tag);
    styleInjectedRef.current = true;
  }, []);

  // Reset when gameId changes or sequence appears to restart (e.g., new round new runtime with small event ids)
  useEffect(() => {
    if (gameId && gameRef.current !== gameId) {
      gameRef.current = gameId;
      lastSeqRef.current = 0;
      queueRef.current = [];
      playingRef.current = false;
    }
    // Detect server-side log reset via decreasing lastEventSeq
    if (view?.lastEventSeq !== undefined && view.lastEventSeq < lastSeqRef.current) {
      lastSeqRef.current = 0;
      queueRef.current = [];
      playingRef.current = false;
    }
    if (!view?.events) return;
    const fresh = view.events.filter(e => e.type === 'PLAY' && e.id > lastSeqRef.current);
    if (fresh.length === 0) return;
    lastSeqRef.current = Math.max(...fresh.map(e => e.id));
    fresh.forEach(e => queueRef.current.push(e));
    if (!playingRef.current) playNext();
  }, [view, gameId]);

  // Draw / penalty draw animations (not queued with play overlay, light-weight flash)
  useEffect(() => {
    if (!view?.events) return;
    for (const ev of view.events) {
      if ((ev.type === 'DRAW' || ev.type === 'PENALTY_DRAW') && !processedDrawRef.current.has(ev.id)) {
        processedDrawRef.current.add(ev.id);
        animateDraw(ev);
      }
    }
  }, [view]);

  function playNext() {
    const ev = queueRef.current.shift();
    if (!ev) { playingRef.current = false; return; }
    playingRef.current = true;
    animateOverlay(ev)
      .then(() => new Promise(r => setTimeout(r, GAP)))
      .finally(() => playNext());
  }

  function animateOverlay(ev) {
    return new Promise(resolve => {
      const discardWrapper = document.querySelector('[data-role="discard"]');
      if (!discardWrapper) return resolve();
      const targetRect = discardWrapper.getBoundingClientRect();

      const cardInfo = parseCardFromText(ev.text || '');

      const overlay = document.createElement('div');
      overlay.className = 'uno-play-overlay animating';
      overlay.style.left = (targetRect.left + targetRect.width / 2) + 'px';
      overlay.style.top = (targetRect.top + targetRect.height / 2) + 'px';

      if (cardInfo.colorStyle) {
        overlay.style.background = cardInfo.colorStyle.bg;
        overlay.style.color = cardInfo.colorStyle.fg;
        if (cardInfo.colorStyle.border) overlay.style.borderColor = cardInfo.colorStyle.border;
      } else {
        overlay.style.background = '#444';
      }
      overlay.textContent = cardInfo.label;
      const ring = document.createElement('div');
      ring.className = 'ring';
      overlay.appendChild(ring);
      document.body.appendChild(overlay);

      setTimeout(() => { overlay.remove(); resolve(); }, DURATION + 40);
    });
  }

  function animateDraw(ev) {
    const txt = ev.text || '';
    // text pattern examples: "BOT3 drew 1 card" / "P1_HOST drew 12 cards (penalty)"
    const m = txt.match(/^(\S+) drew (\d+)/);
    if (!m) return;
    const playerId = m[1];
    const count = parseInt(m[2], 10) || 1;
    const host = document.querySelector(`[data-player-id="${playerId}"]`);
    if (!host) return;
    const badge = host.querySelector('[title="Hand Size"]');
    if (!badge) return;
    // Determine target (new) and source (old) values for smooth count-up.
    // Current badge text is likely already the new hand size rendered by React.
    const targetVal = parseInt(badge.textContent, 10);
    if (isNaN(targetVal)) return; // non-numeric badge
    const sourceVal = Math.max(0, targetVal - count);
    // Store original final value for potential concurrent updates.
    badge.setAttribute('data-final', String(targetVal));
    badge.classList.add('uno-draw-flash','uno-draw-flash-active');
    const D = 1000; // 1s duration
    const start = performance.now();
    function easeOutQuad(t){ return 1 - (1 - t) * (1 - t); }
    function step(ts){
      const progress = Math.min(1, (ts - start)/D);
      const eased = easeOutQuad(progress);
      const current = Math.round(sourceVal + (targetVal - sourceVal) * eased);
      if (!badge.isConnected) return; // aborted
      badge.textContent = String(current);
      if (progress < 1) {
        requestAnimationFrame(step);
      } else {
        // finalize
        badge.textContent = String(targetVal);
        badge.classList.remove('uno-draw-flash-active');
        setTimeout(()=>badge.classList.remove('uno-draw-flash'), 300);
      }
    }
    // Initialize display at source (so user sees increment start)
    badge.textContent = String(sourceVal);
    requestAnimationFrame(step);
  }

  function parseCardFromText(txt) {
    const out = { label: 'CARD', colorStyle: null };
    const afterPlayed = txt.split('played ').pop() || '';
    let core = afterPlayed.split(' (color')[0].trim();
    const colors = ['RED','GREEN','BLUE','YELLOW'];
    const colorMap = { RED:{bg:'#ef4444',fg:'#fff'}, GREEN:{bg:'#22c55e',fg:'#fff'}, BLUE:{bg:'#3b82f6',fg:'#fff'}, YELLOW:{bg:'#facc15',fg:'#000'} };
    let color = null; let value = core;
    const parts = core.split(/\s+/);
    if (parts.length >= 2 && colors.includes(parts[0])) { color = parts[0]; value = parts.slice(1).join('_'); }
    value = value.replace(/\s+/g,'_');
    let label = value;
    if (label === 'DRAW_TWO') label = '+2';
    else if (label === 'WILD_DRAW_FOUR') label = '+4';
    else if (label === 'REVERSE') label = '↺';
    else if (label === 'SKIP') label = '⦸';
    else if (label === 'WILD') label = 'WILD';
    if (!color && value.startsWith('WILD')) {
      out.colorStyle = { bg: 'linear-gradient(135deg,#111,#000,#333)', fg:'#fff', border:'#facc15' };
    } else if (color && colorMap[color]) {
      out.colorStyle = { ...colorMap[color] };
    }
    out.label = label;
    return out;
  }
}
