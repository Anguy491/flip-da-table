import { useState, useContext, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { LoginApi } from '../api/auth';
import { AuthContext } from '../context/auth-context';
import PageContainer from '../components/PageContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { ArcadePanel, StatusBanner } from '../components/arcade/ArcadeUI';
import GoogleSignInButton from '../components/GoogleSignInButton';
import useAuthCapabilities from '../hooks/useAuthCapabilities';
import { clearAuthFragment, readAuthFragment } from '../utils/authFragment';

function Login({ previewCapabilities = null }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { setToken } = useContext(AuthContext);
  const capabilities = useAuthCapabilities(previewCapabilities);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [notice] = useState(location.state?.notice || '');

  useEffect(() => {
    const fragment = readAuthFragment();
    clearAuthFragment();
    if (fragment.googleError) setError('Google sign-in could not be completed. Please try again.');
  }, []);

  // Handle login form submission
  const handleLogin = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const { token } = await LoginApi({ email, password });
      setToken(token);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  // Render login form UI
  return (
    <PageContainer>
      <div className="arcade-auth">
        <section className="arcade-auth__hero arcade-auth__hero--login" aria-labelledby="brand-title">
          <div>
            <h1 id="brand-title" className="arcade-wordmark">Flip Da Table</h1>
            <p className="arcade-copy arcade-auth__tagline">Online Multiplayer Tabletop Game Platform.</p>
          </div>
        </section>
        <ArcadePanel className="arcade-auth__form" aria-labelledby="login-title">
          <form className="arcade-form-stack" onSubmit={handleLogin}>
            <div>
              <h2 id="login-title" className="arcade-title">Continue game</h2>
              <p className="arcade-copy mt-3">Sign in to create a room or rejoin your table.</p>
            </div>
            <FormInput type="email" label="Email" placeholder="player@example.com" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            <FormInput type="password" label="Password" placeholder="Enter password" autoComplete="current-password" maxLength={64} value={password} onChange={(e) => setPassword(e.target.value)} required />
            <ErrorPopup message={error} />
            <SubmitButton fullWidth loading={submitting}>Start</SubmitButton>
            {notice && <StatusBanner tone="success" live>{notice}</StatusBanner>}
            {capabilities.passwordReset && <p className="arcade-copy text-sm text-center"><Link to="/forgot-password">Forgot password?</Link></p>}
            <GoogleSignInButton capability={capabilities.google} />
            <div className="arcade-auth-links">
              <p className="arcade-copy text-sm">New player? <Link to="/register">Create an account</Link></p>
              <Link to="/privacy">Privacy</Link>
            </div>
          </form>
        </ArcadePanel>
      </div>
    </PageContainer>
  );
}

export default Login;
