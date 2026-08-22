import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../context/auth-context';
import Login from './Login';
import Register from './Register';
import Dashboard from './Dashboard';
import Lobby from './Lobby';
import SessionSummary from './SessionSummary';
import { dashboardFixture, lobbyFixture, summaryFixture } from '../dev/fixtures';
import { LoginApi, RegisterApi } from '../api/auth';

vi.mock('../api/auth', () => ({
  LoginApi: vi.fn(),
  RegisterApi: vi.fn(),
}));

function renderPage(page, token = 'preview-token') {
  return render(
    <AuthContext.Provider value={{ token, setToken: vi.fn() }}>
      <MemoryRouter>{page}</MemoryRouter>
    </AuthContext.Provider>,
  );
}

afterEach(() => {
  vi.clearAllMocks();
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

  it('keeps the dashboard focused on the two supported games and opens its join dialog', () => {
    renderPage(<Dashboard preview={dashboardFixture} />);

    expect(screen.getAllByRole('radio')).toHaveLength(2);
    expect(screen.queryByText(/Bounty/i)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Join code' }));
    expect(screen.getByRole('dialog', { name: 'Join a room' })).toBeVisible();
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
