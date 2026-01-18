import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { LoginApi } from '../api/auth';
import { AuthContext } from '../context/AuthContext';
import PageContainer from '../components/PageContainer';
import AsciiBackground from '../components/AsciiBackground';
import PixelFrame from '../components/PixelFrame';
import PixelInput from '../components/PixelInput';
import ArcadeButton from '../components/ArcadeButton';
import PixelAlert from '../components/PixelAlert';

function Login() {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(false);

  // Handle login form submission
  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const { token } = await LoginApi({ email, password });
      setToken(token); // persist via context
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    }
  };

  // Render login form UI
  const emailStatus = email ? 'ok' : 'idle';
  const passwordStatus = password ? 'ok' : 'idle';

  return (
    <PageContainer className="relative overflow-hidden bg-[var(--pixel-bg)] pixel-arcade py-10">
      <AsciiBackground seed="flip-da-table" opacity={0.2} density={0.8} />
      <div className="pixel-crt" aria-hidden="true" />
      <div className="relative z-10 w-full max-w-2xl space-y-6">
        <PixelFrame className="pixel-banner">
          <div className="space-y-2">
            <h1 className="pixel-heading text-2xl sm:text-3xl">Login</h1>
            <p className="text-sm text-slate-600">Server Connected</p>
          </div>
        </PixelFrame>

        <form onSubmit={handleLogin} className="space-y-4">
          <PixelFrame className="p-5 sm:p-6 space-y-4">
            <div className="space-y-3">
              <PixelInput
                label="Email"
                type="email"
                placeholder="Enter email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                status={emailStatus}
              />
              <PixelInput
                label="Password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                status={passwordStatus}
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
            </div>
            <div className="flex flex-col gap-3 text-xs uppercase tracking-[0.2em] text-slate-500 sm:flex-row sm:items-center sm:justify-between">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                  className="pixel-checkbox"
                />
                Remember me
              </label>
            </div>
            <PixelAlert message={error} onDismiss={() => setError('')} />
            <div className="grid gap-3 sm:grid-cols-2">
              <ArcadeButton type="submit" variant="primary">
                Enter
              </ArcadeButton>
              <ArcadeButton type="button" variant="secondary" onClick={() => navigate('/register')}>
                Create Account
              </ArcadeButton>
            </div>
          </PixelFrame>
        </form>

      </div>
    </PageContainer>
  );
}

export default Login;
