import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/auth-context';
import PageContainer from '../components/PageContainer';
import ErrorPopup from '../components/ErrorPopup';
import {
  ArcadeBadge,
  ArcadeButton,
  ArcadeDialog,
  ArcadeInput,
  ArcadePanel,
} from '../components/arcade/ArcadeUI';
import { createSession, joinSession } from '../api/sessions';
import { getUserInfo, updateUserInfo } from '../api/user';
import unoCover from '../assets/uno-arcade.svg';
import dvcCover from '../assets/dvc-code.svg';
import lasVegasCover from '../assets/las-vegas-dice.svg';

const GAMES = [
  {
    gameType: 'UNO',
    name: 'UNO',
    tagline: 'Color, chaos, and one last card.',
    players: '2-10 players',
    maxPlayers: 10,
    img: unoCover,
  },
  {
    gameType: 'DAVINCI',
    name: 'Da Vinci Code',
    tagline: 'Crack the sequence before they crack yours.',
    players: '2-4 players',
    maxPlayers: 4,
    img: dvcCover,
  },
  {
    gameType: 'LASVEGAS',
    name: 'Las Vegas',
    tagline: 'Roll every die. Read the table. Break every tie.',
    players: '3-10 players',
    maxPlayers: 10,
    img: lasVegasCover,
  },
];

export default function Dashboard({ preview = null }) {
  const { token, setToken } = useContext(AuthContext);
  const navigate = useNavigate();
  const [selected, setSelected] = useState(GAMES[0]);
  const [showJoin, setShowJoin] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [joinSessionId, setJoinSessionId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [me, setMe] = useState(preview?.me || null);
  const [form, setForm] = useState({ nickname: preview?.me?.nickname || '', password: '' });

  useEffect(() => {
    if (preview) return undefined;
    if (!token) {
      navigate('/login');
      return undefined;
    }
    let alive = true;
    getUserInfo(token)
      .then((info) => {
        if (!alive) return;
        setMe(info);
        setForm((current) => ({ ...current, nickname: info?.nickname || '' }));
      })
      .catch(() => {
        if (alive) setError('Player profile is temporarily unavailable.');
      });
    return () => { alive = false; };
  }, [token, navigate, preview]);

  const handleLogout = () => {
    setToken(null);
    navigate('/login');
  };

  const createRoom = async () => {
    if (!selected || !token) return;
    setSubmitting(true);
    setError('');
    try {
      const { sessionId } = await createSession({ gameType: selected.gameType, maxPlayers: selected.maxPlayers }, token);
      await joinSession(sessionId, token);
      navigate(`/lobby/${sessionId}`);
    } catch (requestError) {
      setError(requestError.message || 'Failed to create the room.');
    } finally {
      setSubmitting(false);
    }
  };

  const joinRoom = async () => {
    const id = joinSessionId.trim();
    if (!id) return;
    setSubmitting(true);
    setError('');
    try {
      await joinSession(id, token);
      setShowJoin(false);
      navigate(`/lobby/${id}`);
    } catch (requestError) {
      setError(requestError.message || 'Failed to join the room.');
    } finally {
      setSubmitting(false);
    }
  };

  const saveProfile = async () => {
    if (!form.nickname.trim()) return;
    setSubmitting(true);
    setError('');
    try {
      const passwordChanged = Boolean(form.password.trim());
      const payload = { nickname: form.nickname.trim() };
      if (passwordChanged) payload.password = form.password.trim();
      const info = await updateUserInfo(payload, token);
      if (passwordChanged) {
        setToken(null);
        navigate('/login', { replace: true, state: { notice: 'Password updated. Sign in again with the new password.' } });
        return;
      }
      setMe(info);
      setShowEdit(false);
    } catch (requestError) {
      setError(requestError.message || 'Profile update failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <div className="arcade-dashboard-layout">
        <header className="arcade-dashboard-header">
          <div>
            <p className="arcade-eyebrow">Main floor // three cabinets online</p>
            <h1 className="arcade-title">Choose your table</h1>
            <p className="arcade-copy mt-3">Welcome back, <strong className="arcade-accent">{me?.nickname || 'player'}</strong>. Pick a game, open a room, and send the code.</p>
          </div>
          <div className="arcade-actions">
            <ArcadeButton variant="ghost" size="small" onClick={() => {
              setError('');
              setForm({ nickname: me?.nickname || '', password: '' });
              setShowEdit(true);
            }}>Profile</ArcadeButton>
            <ArcadeButton variant="ghost" size="small" onClick={handleLogout}>Log out</ArcadeButton>
          </div>
        </header>

        <div className="arcade-game-grid" role="radiogroup" aria-label="Game selection">
          {GAMES.map((game) => {
            const active = selected.gameType === game.gameType;
            return (
              <button
                key={game.gameType}
                type="button"
                role="radio"
                aria-checked={active}
                className={`arcade-game-card ${active ? 'arcade-game-card--selected' : ''}`}
                onClick={() => setSelected(game)}
              >
                <img src={game.img} alt="" />
                <span className="arcade-game-card__body">
                  <span className="arcade-game-card__title">{game.name}</span>
                  <span className="arcade-copy">{game.tagline}</span>
                  <ArcadeBadge tone={active ? 'success' : 'muted'}>{active ? 'Selected' : game.players}</ArcadeBadge>
                </span>
              </button>
            );
          })}
        </div>

        <ArcadePanel quiet className="flex flex-col md:flex-row items-start md:items-center justify-between gap-5">
          <div>
            <p className="arcade-eyebrow">Ready player</p>
            <h2 className="text-xl font-bold">{selected.name}</h2>
            <p className="arcade-copy mt-2">A new room supports {selected.players}. You will be the host.</p>
          </div>
          <div className="arcade-actions shrink-0">
            <ArcadeButton variant="secondary" onClick={() => { setError(''); setJoinSessionId(''); setShowJoin(true); }}>Join code</ArcadeButton>
            <ArcadeButton loading={submitting} onClick={createRoom}>Create room</ArcadeButton>
          </div>
        </ArcadePanel>
        <ErrorPopup message={error} />
      </div>

      <ArcadeDialog
        open={showJoin}
        title="Join a room"
        eyebrow="Enter invite code"
        onClose={() => setShowJoin(false)}
        actions={(
          <>
            <ArcadeButton variant="ghost" onClick={() => setShowJoin(false)}>Cancel</ArcadeButton>
            <ArcadeButton loading={submitting} disabled={!joinSessionId.trim()} onClick={joinRoom}>Join table</ArcadeButton>
          </>
        )}
      >
        <div className="arcade-form-stack">
          <ArcadeInput label="Session ID" placeholder="Paste room code" value={joinSessionId} onChange={(event) => setJoinSessionId(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') joinRoom(); }} />
          <ErrorPopup message={error} />
        </div>
      </ArcadeDialog>

      <ArcadeDialog
        open={showEdit}
        title="Player profile"
        eyebrow="Edit identity"
        onClose={() => setShowEdit(false)}
        actions={(
          <>
            <ArcadeButton variant="ghost" onClick={() => setShowEdit(false)}>Cancel</ArcadeButton>
            <ArcadeButton loading={submitting} disabled={!form.nickname.trim()} onClick={saveProfile}>Save player</ArcadeButton>
          </>
        )}
      >
        <div className="arcade-form-stack">
          <ArcadeInput label="Nickname" maxLength={32} value={form.nickname} onChange={(event) => setForm((current) => ({ ...current, nickname: event.target.value }))} />
          <ArcadeInput label="New password" hint="Leave blank to keep the current password." type="password" autoComplete="new-password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} />
          <ErrorPopup message={error} />
        </div>
      </ArcadeDialog>
    </PageContainer>
  );
}
