import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../context/auth-context';
import Login from './Login';
import Register from './Register';
import Dashboard from './Dashboard';
import Lobby from './Lobby';
import SessionSummary from './SessionSummary';
import ForgotPassword from './ForgotPassword';
import ResetPassword from './ResetPassword';
import GoogleAuthCallback from './GoogleAuthCallback';
import { dashboardFixture, lobbyFixture, summaryFixture } from '../dev/fixtures';
import {
  ExchangeGoogleCodeApi,
  ForgotPasswordApi,
  GetAuthCapabilitiesApi,
  LinkGoogleAccountApi,
  LoginApi,
  RegisterApi,
  ResetPasswordApi,
} from '../api/auth';

vi.mock('../api/auth', () => ({
  LoginApi: vi.fn(),
  RegisterApi: vi.fn(),
  GetAuthCapabilitiesApi: vi.fn(() => Promise.resolve({
    passwordReset: true,
    supportEmail: 'support@anguy.dev',
    google: { enabled: false, clientId: '', loginUri: '' },
  })),
  ForgotPasswordApi: vi.fn(),
  ResetPasswordApi: vi.fn(),
  ExchangeGoogleCodeApi: vi.fn(() => Promise.resolve({ token: 'application-token' })),
  LinkGoogleAccountApi: vi.fn(),
}));

function renderPage(page, token = 'preview-token', setToken = vi.fn()) {
  return render(
    <AuthContext.Provider value={{ token, setToken }}>
      <MemoryRouter>{page}</MemoryRouter>
    </AuthContext.Provider>,
  );
}

afterEach(() => {
  vi.clearAllMocks();
  window.history.replaceState({}, '', '/');
});

describe('public and shared page states', () => {
  it('submits login credentials through the semantic form and announces API errors', async () => {
    LoginApi.mockRejectedValueOnce(new Error('Invalid player credentials.'));
    renderPage(<Login />, null);

    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'player@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong-pass' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Start' }).closest('form'));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Invalid player credentials.'));
    expect(LoginApi).toHaveBeenCalledWith({ email: 'player@example.com', password: 'wrong-pass' });
  });

  it('blocks registration when password confirmation does not match', () => {
    renderPage(<Register />, null);
    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'player@example.com' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Nickname' }), { target: { value: 'PixelPilot' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'arcade-pass' } });
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'different-pass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create player' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Passwords do not match');
    expect(RegisterApi).not.toHaveBeenCalled();
  });

  it('shows the same successful recovery state after a forgot-password request', async () => {
    ForgotPasswordApi.mockResolvedValueOnce({ message: 'accepted' });
    renderPage(<ForgotPassword />, null);

    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'player@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send reset link' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/if an account exists/i));
    expect(ForgotPasswordApi).toHaveBeenCalledWith({ email: 'player@example.com' });
  });

  it('consumes a password reset token from the fragment and clears it from the URL', async () => {
    window.history.replaceState({}, '', '/reset-password#token=one-time-reset');
    ResetPasswordApi.mockResolvedValueOnce({});
    renderPage(<ResetPassword />, null);

    expect(window.location.hash).toBe('');
    fireEvent.change(screen.getByLabelText('New password', { selector: 'input' }), { target: { value: 'new-arcade-pass' } });
    fireEvent.change(screen.getByLabelText('Confirm password', { selector: 'input' }), { target: { value: 'new-arcade-pass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/older sign-ins are now invalid/i));
    expect(ResetPasswordApi).toHaveBeenCalledWith({ token: 'one-time-reset', newPassword: 'new-arcade-pass' });
  });

  it('does not submit a reset when password confirmation differs', () => {
    renderPage(<ResetPassword previewToken="one-time-reset" />, null);
    fireEvent.change(screen.getByLabelText('New password', { selector: 'input' }), { target: { value: 'new-arcade-pass' } });
    fireEvent.change(screen.getByLabelText('Confirm password', { selector: 'input' }), { target: { value: 'different-pass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Passwords do not match');
    expect(ResetPasswordApi).not.toHaveBeenCalled();
  });

  it('exchanges a Google handoff from the fragment and stores the application token', async () => {
    window.history.replaceState({}, '', '/auth/google/callback#code=google-handoff');
    ExchangeGoogleCodeApi.mockResolvedValueOnce({ token: 'application-token' });
    const setToken = vi.fn();
    renderPage(<GoogleAuthCallback />, null, setToken);

    await waitFor(() => expect(setToken).toHaveBeenCalledWith('application-token'));
    expect(window.location.hash).toBe('');
    expect(ExchangeGoogleCodeApi).toHaveBeenCalledWith({ code: 'google-handoff' });
  });

  it('requires the original password for a third-party Google account collision', async () => {
    window.history.replaceState({}, '', '/auth/google/callback#link=link-handoff');
    LinkGoogleAccountApi.mockRejectedValueOnce(new Error('username or password incorrect'));
    renderPage(<GoogleAuthCallback />, null);

    expect(window.location.hash).toBe('');
    expect(screen.getByText(/matches an existing player/i)).toBeVisible();
    fireEvent.change(screen.getByLabelText('Existing account password'), { target: { value: 'wrong-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Link and continue' }));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(/did not match/i));
    expect(LinkGoogleAccountApi).toHaveBeenCalledWith({ code: 'link-handoff', password: 'wrong-password' });
  });

  it('shows the three supported games and opens its join dialog', () => {
    renderPage(<Dashboard preview={dashboardFixture} />);

    expect(screen.getAllByRole('radio')).toHaveLength(3);
    expect(screen.getByRole('radio', { name: /Las Vegas/i })).toBeVisible();
    expect(screen.queryByText(/Bounty/i)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Join code' }));
    expect(screen.getByRole('dialog', { name: 'Join a room' })).toBeVisible();
  });

  it('allows one human to add enough Las Vegas bots while keeping series controls hidden', () => {
    renderPage(<Lobby preview={{
      sessionId: 'VEGAS-ROOM',
      myUserId: 'host-1',
      rounds: 1,
      connectionState: 'connected',
      sessionInfo: {
        gameType: 'LASVEGAS',
        maxPlayers: 10,
        ownerId: 'host-1',
        capabilities: { minPlayers: 3, maxPlayers: 10, botsAllowed: true, seriesAllowed: false, internalRounds: 3 },
      },
      players: [
        { name: 'P1', bot: false, ready: true },
      ],
    }} />);

    const addBot = screen.getByRole('button', { name: '+ Add bot' });
    fireEvent.click(addBot);
    fireEvent.click(addBot);
    expect(screen.queryByRole('combobox', { name: 'Rounds' })).not.toBeInTheDocument();
    expect(screen.getByText('1 platform game / 3 casino rounds')).toBeVisible();
    expect(screen.getByText('Capacity: 3-10')).toBeVisible();
    expect(screen.getByText('3/3 ready')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Start game' })).toBeEnabled();
  });

  it('renders a maximum-capacity lobby and prevents an invalid start', () => {
    renderPage(<Lobby preview={lobbyFixture} />);

    expect(screen.getAllByRole('article')).toHaveLength(10);
    expect(screen.getByRole('button', { name: '+ Add bot' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Start game' })).toBeDisabled();
    expect(screen.getByText('9/10 ready')).toBeVisible();
  });

  it('renders ties, long names, and players below the podium in the summary', () => {
    renderPage(<SessionSummary previewData={summaryFixture} previewSessionId="ARCADE-ROOM-8BIT-2048" />);

    expect(screen.getByRole('heading', { name: 'Final scoreboard' })).toBeVisible();
    expect(screen.getAllByText('2')).toHaveLength(2);
    expect(screen.getAllByText('LongNicknameThatNeedsTruncation')).toHaveLength(2);
    expect(screen.getByText('Bot 1: 1')).toBeVisible();
  });
});
