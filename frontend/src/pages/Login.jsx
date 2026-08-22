import { useState, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { LoginApi } from '../api/auth';
import { AuthContext } from '../context/auth-context';
import PageContainer from '../components/PageContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { ArcadePanel } from '../components/arcade/ArcadeUI';

function Login() {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

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
            <FormInput type="password" label="Password" placeholder="Enter password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <ErrorPopup message={error} />
            <SubmitButton fullWidth loading={submitting}>Start</SubmitButton>
            <p className="arcade-copy text-sm text-center">New player? <Link to="/register">Create an account</Link></p>
          </form>
        </ArcadePanel>
      </div>
    </PageContainer>
  );
}

export default Login;
