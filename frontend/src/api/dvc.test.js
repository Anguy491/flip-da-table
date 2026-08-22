import { afterEach, describe, expect, it, vi } from 'vitest';
import { settle } from './dvc';

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('DVC API', () => {
  it('does not report a rejected rack order as successfully locked', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue(false),
    }));

    await expect(settle('game-1', 'P1', 'B1≤W2≤', true, 'token'))
      .rejects.toThrow('server rejected this rack order');
  });
});
