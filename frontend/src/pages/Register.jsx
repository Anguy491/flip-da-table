import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { RegisterApi } from '../api/auth';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import AsciiBackground from '../components/AsciiBackground';
import PixelFrame from '../components/PixelFrame';
import PixelInput from '../components/PixelInput';
import ArcadeButton from '../components/ArcadeButton';
import PixelAlert from '../components/PixelAlert';

function Register() {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const [email, setEmail] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  // Handle register form submission
  const handleRegister = async (e) => {
    e.preventDefault();

    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }

    try {
      const { token } = await RegisterApi({ email, password, nickname });
      setToken(token); // persist via context
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    }
  };

  // Render register form UI
  const emailOk = email.includes('@');
  const nicknameOk = nickname.trim().length >= 2;
  const passwordOk = password.length >= 6;
  const confirmOk = confirm.length > 0 && confirm === password;
  const terminalLine = error
    ? `> error: ${error}`
    : confirm && !confirmOk
      ? '> password mismatch'
      : password && !passwordOk
        ? '> password strength: weak'
        : email && !emailOk
          ? '> email invalid'
          : '> validating...';

  return (
    <PageContainer className="relative overflow-hidden bg-[var(--pixel-bg)] pixel-arcade py-10">
      <AsciiBackground seed="flip-da-table" opacity={0.2} density={0.8} />
      <div className="pixel-crt" aria-hidden="true" />
      <div className="relative z-10 w-full max-w-2xl space-y-6">
        <PixelFrame className="pixel-banner">
          <div className="space-y-2">
            <h1 className="pixel-heading text-2xl sm:text-3xl">Register</h1>
            <p className="text-sm text-slate-600">Welcome to Flip Da Table</p>
          </div>
        </PixelFrame>

        <div className="space-y-4">
          <form onSubmit={handleRegister} className="space-y-4">
            <PixelFrame className="p-5 sm:p-6 space-y-4">
              <div className="space-y-3">
                <PixelInput
                  label="Nickname"
                  type="text"
                  placeholder="Display name"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  required
                  autoComplete="nickname"
                  status={nickname ? (nicknameOk ? 'ok' : 'error') : 'idle'}
                />
                <PixelInput
                  label="Email"
                  type="email"
                  placeholder="Email address"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                  status={email ? (emailOk ? 'ok' : 'error') : 'idle'}
                />
                <PixelInput
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Create password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                  status={password ? (passwordOk ? 'ok' : 'error') : 'idle'}
                  rightElement={
                    <button
                      type="button"
                      className="pixel-input-toggle flex-shrink-0"
                      onClick={() => setShowPassword((prev) => !prev)}
                    >
                      {showPassword ? 'HIDE' : 'SHOW'}
                    </button>
                  }
                />
                <PixelInput
                  label="Confirm Password"
                  type={showConfirm ? 'text' : 'password'}
                  placeholder="Repeat password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  required
                  autoComplete="new-password"
                  status={confirm ? (confirmOk ? 'ok' : 'error') : 'idle'}
                  rightElement={
                    <button
                      type="button"
                      className="pixel-input-toggle flex-shrink-0"
                      onClick={() => setShowConfirm((prev) => !prev)}
                    >
                      {showConfirm ? 'HIDE' : 'SHOW'}
                    </button>
                  }
                />
              </div>
              <div className="text-xs uppercase tracking-[0.2em] text-slate-500">
                {terminalLine}
              </div>
              <PixelAlert message={error} onDismiss={() => setError('')} />
              <div className="grid gap-3 sm:grid-cols-2">
                <ArcadeButton type="submit" variant="primary">
                  Create
                </ArcadeButton>
                <ArcadeButton type="button" variant="secondary" onClick={() => navigate('/login')}>
                  Back to Login
                </ArcadeButton>
              </div>
            </PixelFrame>
          </form>
        </div>

      </div>
    </PageContainer>
  );
}

export default Register;
