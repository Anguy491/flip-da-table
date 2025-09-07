import React, { useEffect, useRef, useState } from 'react';
import { CardStrip } from './CardStrip';

// validity rule: all revealed numbers (non-hidden) must be non-decreasing left->right (simple example placeholder)
export function isArrangementValid(cards) {
  // Rules:
  // 1) Numbers must be non-decreasing left -> right (ignoring jokers)
  // 2) When numbers are equal, BLACK must be to the left of WHITE
  //    (i.e., for equal v, sequence ... B(v) ... W(v) is valid; W(v) left of B(v) is invalid)
  let prevVal = -1;
  let prevColor = null; // 'BLACK' | 'WHITE' for the last non-joker card
  for (const c of cards) {
    if (c?.isJoker) continue; // joker can be anywhere
    const v = typeof c?.value === 'number' ? c.value : parseInt(c?.value, 10);
    if (Number.isNaN(v)) continue; // skip unknown
    const color = c?.color; // 'BLACK' | 'WHITE'
    // Non-decreasing check
    if (v < prevVal) return false;
    // Equal number tie-breaker: black must be before white
    if (v === prevVal && prevColor && color) {
      if (prevColor === 'WHITE' && color === 'BLACK') return false;
    }
    prevVal = v;
    prevColor = color;
  }
  return true;
}

export function MyHandPanel({ cards, draggable, onReorder, showValidity=false }) {
  const valid = isArrangementValid(cards);
  const scrollerRef = useRef(null);
  const [fadeLeft, setFadeLeft] = useState(false);
  const [fadeRight, setFadeRight] = useState(false);

  const updateFades = () => {
    const el = scrollerRef.current; if (!el) return;
    const { scrollLeft, scrollWidth, clientWidth } = el;
    const maxScroll = Math.max(0, scrollWidth - clientWidth);
    setFadeLeft(scrollLeft > 0);
    setFadeRight(scrollLeft < maxScroll - 1);
  };

  useEffect(() => {
    updateFades();
    const el = scrollerRef.current;
    if (!el) return;
    const onScroll = () => updateFades();
    el.addEventListener('scroll', onScroll, { passive: true });
    const onResize = () => updateFades();
    window.addEventListener('resize', onResize);
    return () => { el.removeEventListener('scroll', onScroll); window.removeEventListener('resize', onResize); };
  }, []);
  return (
    <div className="dvc-myhand">
      <div className="flex items-center justify-between mb-1">
        <h3 className="text-xs font-semibold">My Hand</h3>
        {showValidity && <span className={`text-[10px] ${valid?'text-success':'text-error'}`}>{valid?'OK':'Invalid'}</span>}
      </div>
      <div className="relative">
        <div ref={scrollerRef} className="overflow-x-auto pb-1">
          <div className="w-max">
            <CardStrip cards={cards} draggable={draggable} onReorder={onReorder} />
          </div>
        </div>
        {fadeLeft && (
          <div className="pointer-events-none absolute left-0 top-0 bottom-0 w-6 bg-gradient-to-r from-base-200/80 to-transparent" />
        )}
        {fadeRight && (
          <div className="pointer-events-none absolute right-0 top-0 bottom-0 w-6 bg-gradient-to-l from-base-200/80 to-transparent" />
        )}
      </div>
    </div>
  );
}
