import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ResetPasswordApi } from '../api/auth';
import PageContainer from '../components/PageContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { ArcadePanel, StatusBanner } from '../components/arcade/ArcadeUI';
import { clearAuthFragment, readAuthFragment } from '../utils/authFragment';

export default function ResetPassword({ previewToken = '' }) {
  const [token] = useState(() => previewToken || readAuthFragment().resetToken);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [complete, setComplete] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => clearAuthFragment(), []);

  const submit = async (event) => {
    event.preventDefault();
    if (password !== confirm) {
      setError('Passwords do not match');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await ResetPasswordApi({ token, newPassword: password });
      setComplete(true);
    } catch (requestError) {
      setError(requestError.message === 'RESET_TOKEN_INVALID_OR_EXPIRED'
        ? 'This reset link is invalid or has expired.'
        : requestError.message || 'Password reset failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <div className="arcade-auth">
        <section className="arcade-auth__hero" aria-labelledby="reset-brand-title">
          <div>
            <p className="arcade-kicker">Secure checkpoint</p>
            <h1 id="reset-brand-title" className="arcade-wordmark">Set a new key <span>Re-enter the arcade</span></h1>
          </div>
          <p className="arcade-copy max-w-xl">Completing this reset signs out every older game token for your account.</p>
        </section>
        <ArcadePanel className="arcade-auth__form" aria-labelledby="reset-title">
          <form className="arcade-form-stack" onSubmit={submit}>
            <div>
              <p className="arcade-eyebrow">Password recovery</p>
              <h2 id="reset-title" className="arcade-title">New password</h2>
            </div>
            {!token && <StatusBanner tone="error">This reset link is missing its security token.</StatusBanner>}
            {complete ? (
              <>
                <StatusBanner tone="success" live>Your password has been updated. Older sign-ins are now invalid.</StatusBanner>
                <Link className="arcade-button" to="/login">Return to login</Link>
              </>
            ) : (
              <>
                <FormInput type="password" label="New password" minLength={6} maxLength={64} autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} required disabled={!token} />
                <FormInput type="password" label="Confirm password" minLength={6} maxLength={64} autoComplete="new-password" value={confirm} onChange={(event) => setConfirm(event.target.value)} required disabled={!token} />
                <ErrorPopup message={error} />
                <SubmitButton fullWidth loading={submitting} disabled={!token}>Reset password</SubmitButton>
              </>
            )}
            <p className="arcade-copy text-sm text-center"><Link to="/forgot-password">Request another link</Link></p>
          </form>
        </ArcadePanel>
      </div>
    </PageContainer>
  );
}
