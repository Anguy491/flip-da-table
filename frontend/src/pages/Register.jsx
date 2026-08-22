import { useState, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { RegisterApi } from '../api/auth';
import { AuthContext } from '../context/auth-context';
import PageContainer from '../components/PageContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { ArcadePanel } from '../components/arcade/ArcadeUI';
import GoogleSignInButton from '../components/GoogleSignInButton';
import useAuthCapabilities from '../hooks/useAuthCapabilities';

function Register({ previewCapabilities = null }) {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const capabilities = useAuthCapabilities(previewCapabilities);
  const [email, setEmail] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Handle register form submission
  const handleRegister = async (e) => {
    e.preventDefault();

    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const { token } = await RegisterApi({ email, password, nickname });
      setToken(token);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  // Render register form UI
  return (
    <PageContainer>
      <div className="arcade-auth">
        <section className="arcade-auth__hero" aria-labelledby="register-brand-title">
          <div>
            <p className="arcade-kicker">New challenger detected</p>
            <h1 id="register-brand-title" className="arcade-wordmark">Claim a seat <span>Your table is waiting</span></h1>
          </div>
          <p className="arcade-copy max-w-xl">Choose a nickname the whole lobby can read. You can update it later from the dashboard.</p>
        </section>
        <ArcadePanel className="arcade-auth__form" aria-labelledby="register-title">
          <form className="arcade-form-stack" onSubmit={handleRegister}>
            <div>
              <p className="arcade-eyebrow">Create player</p>
              <h2 id="register-title" className="arcade-title">Join the arcade</h2>
            </div>
            <FormInput type="email" label="Email" placeholder="player@example.com" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            <FormInput type="text" label="Nickname" placeholder="Table name" autoComplete="nickname" maxLength={32} value={nickname} onChange={(e) => setNickname(e.target.value)} required />
            <FormInput type="password" label="Password" placeholder="Create password" autoComplete="new-password" minLength={6} maxLength={64} value={password} onChange={(e) => setPassword(e.target.value)} required />
            <FormInput type="password" label="Confirm password" placeholder="Repeat password" autoComplete="new-password" minLength={6} maxLength={64} value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
            <ErrorPopup message={error} />
            <SubmitButton fullWidth loading={submitting}>Create player</SubmitButton>
            <GoogleSignInButton capability={capabilities.google} />
            <div className="arcade-auth-links">
              <p className="arcade-copy text-sm">Already registered? <Link to="/login">Return to login</Link></p>
              <Link to="/privacy">Privacy</Link>
            </div>
          </form>
        </ArcadePanel>
      </div>
    </PageContainer>
  );
}

export default Register;
