import { useContext, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ExchangeGoogleCodeApi, LinkGoogleAccountApi } from '../api/auth';
import { AuthContext } from '../context/auth-context';
import PageContainer from '../components/PageContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { ArcadePanel, StatusBanner } from '../components/arcade/ArcadeUI';
import { clearAuthFragment, readAuthFragment } from '../utils/authFragment';

export default function GoogleAuthCallback() {
  const navigate = useNavigate();
  const { setToken } = useContext(AuthContext);
  const [handoff] = useState(() => readAuthFragment());
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(Boolean(handoff.code));
  const [error, setError] = useState('');

  useEffect(() => {
    clearAuthFragment();
    if (!handoff.code) return undefined;
    let active = true;
    ExchangeGoogleCodeApi({ code: handoff.code })
      .then(({ token }) => {
        if (!active) return;
        setToken(token);
        navigate('/dashboard', { replace: true });
      })
      .catch(() => {
        if (active) {
          setError('This Google sign-in handoff is invalid or has expired.');
          setSubmitting(false);
        }
      });
    return () => { active = false; };
  }, [handoff.code, navigate, setToken]);

  const linkAccount = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const { token } = await LinkGoogleAccountApi({ code: handoff.link, password });
      setToken(token);
      navigate('/dashboard', { replace: true });
    } catch (requestError) {
      setError(requestError.message === 'username or password incorrect'
        ? 'The password did not match the existing account.'
        : 'This account-link request is invalid or has expired.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <ArcadePanel className="arcade-auth-callback" aria-labelledby="google-callback-title">
        <div className="arcade-form-stack">
          <div>
            <p className="arcade-eyebrow">Google identity checkpoint</p>
            <h1 id="google-callback-title" className="arcade-title">Complete sign-in</h1>
          </div>
          {handoff.code && submitting && <StatusBanner live>Verifying the one-time Google sign-in code...</StatusBanner>}
          {handoff.link && (
            <form className="arcade-form-stack" onSubmit={linkAccount}>
              <StatusBanner tone="warning">This third-party Google email matches an existing player. Enter the original player password to link safely.</StatusBanner>
              <FormInput type="password" label="Existing account password" autoComplete="current-password" maxLength={64} value={password} onChange={(event) => setPassword(event.target.value)} required />
              <ErrorPopup message={error} />
              <SubmitButton fullWidth loading={submitting}>Link and continue</SubmitButton>
              <p className="arcade-copy text-sm">Forgot the original password? <Link to="/forgot-password">Reset it first</Link>, then restart Google sign-in.</p>
            </form>
          )}
          {!handoff.code && !handoff.link && !error && <StatusBanner tone="error">No Google sign-in handoff was found.</StatusBanner>}
          {error && !handoff.link && <ErrorPopup message={error} />}
          <p className="arcade-copy text-sm text-center"><Link to="/login">Return to login</Link></p>
        </div>
      </ArcadePanel>
    </PageContainer>
  );
}
