import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GoogleSignInButton from './GoogleSignInButton';

describe('GoogleSignInButton', () => {
  afterEach(() => {
    delete window.google;
    document.getElementById('google-identity-services')?.remove();
    vi.restoreAllMocks();
  });

  it('asks Google Identity Services to render the official redirect button without One Tap', async () => {
    const initialize = vi.fn();
    const renderButton = vi.fn();
    window.google = { accounts: { id: { initialize, renderButton } } };

    render(<GoogleSignInButton capability={{
      enabled: true,
      clientId: 'web-client-id',
      loginUri: 'https://game.anguy.dev/api/auth/google/callback',
    }} />);

    await waitFor(() => expect(renderButton).toHaveBeenCalledTimes(1));
    expect(initialize).toHaveBeenCalledWith(expect.objectContaining({
      client_id: 'web-client-id',
      ux_mode: 'redirect',
      auto_select: false,
    }));
    expect(window.google.accounts.id.prompt).toBeUndefined();
  });

  it('keeps email login available and announces an SDK loading failure', async () => {
    render(<GoogleSignInButton capability={{
      enabled: true,
      clientId: 'web-client-id',
      loginUri: 'https://game.anguy.dev/api/auth/google/callback',
    }} />);

    document.getElementById('google-identity-services').dispatchEvent(new Event('error'));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/still use email and password/i));
    expect(document.getElementById('google-identity-services')).toBeNull();
  });
});
