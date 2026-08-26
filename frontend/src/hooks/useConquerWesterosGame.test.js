import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useConquerWesterosGame from './useConquerWesterosGame';
import { getConquerWesterosView, sendConquerWesterosCommand } from '../api/conquerWesteros';
import { conquerWesterosFixture } from '../dev/fixtures';

vi.mock('../api/conquerWesteros', () => ({
  getConquerWesterosView: vi.fn(),
  sendConquerWesterosCommand: vi.fn(),
}));

vi.mock('../api/sessions', () => ({ getLatestGame: vi.fn() }));

vi.mock('@stomp/stompjs', () => ({
  Client: class {
    activate() {}
    deactivate() {}
    subscribe() {}
  },
}));

describe('useConquerWesterosGame', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getConquerWesterosView.mockResolvedValue(conquerWesterosFixture);
  });

  it('sends optimistic versions and reloads after a 409 conflict', async () => {
    const stale = Object.assign(new Error('stale'), { status: 409 });
    sendConquerWesterosCommand.mockRejectedValueOnce(stale);
    const { result } = renderHook(() => useConquerWesterosGame({
      sessionId: 'session-1',
      initialGameId: 'game-1',
      initialPlayerId: 'P1',
      initialView: conquerWesterosFixture,
      token: 'token',
    }));
    await waitFor(() => expect(getConquerWesterosView).toHaveBeenCalledOnce());

    act(() => result.current.actions.roll());

    await waitFor(() => expect(sendConquerWesterosCommand).toHaveBeenCalledWith('game-1', {
      expectedVersion: 8,
      type: 'ROLL_DICE',
    }, 'token'));
    await waitFor(() => expect(getConquerWesterosView).toHaveBeenCalledTimes(2));
    expect(result.current.view).toEqual(conquerWesterosFixture);
  });
});
