import { describe, expect, it } from 'vitest';
import { isArrangementValid } from './arrangement';

describe('DVC rack arrangement', () => {
  it('accepts ascending values with black before white on ties', () => {
    expect(isArrangementValid([
      { color: 'BLACK', value: 2 },
      { color: 'WHITE', value: 2 },
      { color: 'BLACK', value: 5 },
    ])).toBe(true);
  });

  it('rejects descending values and reversed tie colors', () => {
    expect(isArrangementValid([{ color: 'BLACK', value: 5 }, { color: 'WHITE', value: 2 }])).toBe(false);
    expect(isArrangementValid([{ color: 'WHITE', value: 4 }, { color: 'BLACK', value: 4 }])).toBe(false);
  });

  it('allows jokers anywhere in the rack', () => {
    expect(isArrangementValid([{ color: 'WHITE', value: 1 }, { isJoker: true }, { color: 'BLACK', value: 8 }])).toBe(true);
  });
});
