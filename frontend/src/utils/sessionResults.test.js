import { beforeEach, describe, expect, it } from 'vitest';
import { readSessionResults, recordSessionResult, sessionResultsKey } from './sessionResults';

describe('session result persistence', () => {
  beforeEach(() => sessionStorage.clear());

  it('stores DVC results in the shared session format and updates a repeated round', () => {
    const input = {
      sessionId: 'dvc-room',
      gameType: 'DAVINCI',
      totalRounds: 1,
      playersMeta: [{ playerId: 'P1', name: 'PixelHost' }],
      result: { round: 1, winnerId: 'P1', winnerName: 'PixelHost', turns: 3 },
    };
    recordSessionResult(input);
    recordSessionResult({ ...input, result: { ...input.result, turns: 4 } });

    expect(readSessionResults('dvc-room')).toMatchObject({ gameType: 'DAVINCI', totalRounds: 1 });
    expect(readSessionResults('dvc-room').results).toEqual([{ ...input.result, turns: 4 }]);
    expect(sessionStorage.getItem(sessionResultsKey('dvc-room'))).toBeTruthy();
  });

  it('continues to read legacy UNO result keys', () => {
    sessionStorage.setItem('uno-results-old-room', JSON.stringify({ totalRounds: 1, results: [] }));
    expect(readSessionResults('old-room')).toMatchObject({ totalRounds: 1 });
  });
});
