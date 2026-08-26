import { afterEach, describe, expect, it, vi } from 'vitest';
import { getConquerWesterosView, sendConquerWesterosCommand } from './conquerWesteros';

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('Conquer Westeros API', () => {
  it('uses the authenticated view endpoint and sends the complete command shape', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue({ phase: 'WAITING_FOR_ROLL' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await getConquerWesterosView('game-1', 'token');
    await sendConquerWesterosCommand('game-1', {
      expectedVersion: 7,
      type: 'COMPLETE_LINE',
      targetId: 'T05',
      lineId: 'L1',
      dieIds: [0, 1],
    }, 'token');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/games/conquer-westeros/game-1/view');
    expect(fetchMock.mock.calls[0][0]).not.toContain('viewerId');
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({
      expectedVersion: 7,
      type: 'COMPLETE_LINE',
      targetId: 'T05',
      lineId: 'L1',
      dieIds: [0, 1],
    });
  });

  it('preserves 409 status for automatic reload handling', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: vi.fn().mockResolvedValue({ error: 'stale version' }),
    }));

    await expect(sendConquerWesterosCommand('game-1', { expectedVersion: 1, type: 'ROLL_DICE' }, 'token'))
      .rejects.toMatchObject({ message: 'stale version', status: 409 });
  });
});
