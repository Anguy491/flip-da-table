import { describe, expect, it } from 'vitest';
import { canPlayUnoCard, parseSseBlock } from './useUnoGame';

describe('UNO playable card mapping', () => {
  it('only enables the matching penalty type while a draw stack is active', () => {
    const state = {
      top: { color: 'RED', value: 'DRAW_TWO' },
      activeColor: 'RED',
      pendingDraw: 2,
      pendingDrawType: 'DRAW_TWO',
    };

    expect(canPlayUnoCard({ color: 'BLUE', value: 'DRAW_TWO' }, state)).toBe(true);
    expect(canPlayUnoCard({ color: 'NONE', value: 'WILD_DRAW_FOUR' }, state)).toBe(false);
    expect(canPlayUnoCard({ color: 'RED', value: 'SKIP' }, state)).toBe(false);
  });

  it('falls back to the visible penalty card for older compatible views', () => {
    const state = {
      top: { color: 'NONE', value: 'WILD_DRAW_FOUR' },
      activeColor: 'BLUE',
      pendingDraw: 4,
    };

    expect(canPlayUnoCard({ color: 'NONE', value: 'WILD_DRAW_FOUR' }, state)).toBe(true);
    expect(canPlayUnoCard({ color: 'BLUE', value: 'DRAW_TWO' }, state)).toBe(false);
  });
});

describe('UNO authenticated event stream parsing', () => {
  it('parses a named JSON view event', () => {
    expect(parseSseBlock('event:VIEW\ndata:{"phase":"RUNTIME"}')).toEqual({
      event: 'VIEW',
      data: '{"phase":"RUNTIME"}',
    });
  });
});
