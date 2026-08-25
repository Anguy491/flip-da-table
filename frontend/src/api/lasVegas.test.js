import { afterEach, describe, expect, it, vi } from 'vitest';
import { getLasVegasView, sendLasVegasCommand, setLasVegasAssetVisibility } from './lasVegas';

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('Las Vegas API', () => {
  it('derives the viewer from authentication and sends optimistic versions', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue({ phase: 'WAITING_FOR_ROLL' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await getLasVegasView('game-1', 'token');
    await sendLasVegasCommand('game-1', { expectedVersion: 7, type: 'PLACE_DICE', face: 4 }, 'token');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/games/las-vegas/game-1/view');
    expect(fetchMock.mock.calls[0][0]).not.toContain('viewerId');
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ expectedVersion: 7, type: 'PLACE_DICE', face: 4 });
  });

  it('keeps asset visibility in the presentation endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue({}),
    });
    vi.stubGlobal('fetch', fetchMock);

    await setLasVegasAssetVisibility('game-2', true, 'token');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/games/las-vegas/game-2/presentation/assets');
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ visible: true });
  });
});
