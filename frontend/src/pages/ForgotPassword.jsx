import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ForgotPasswordApi } from '../api/auth';
import PageContainer from '../components/PageContainer';
import FormInput from '../components/FormInput';
import SubmitButton from '../components/SubmitButton';
import ErrorPopup from '../components/ErrorPopup';
import { ArcadePanel, StatusBanner } from '../components/arcade/ArcadeUI';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await ForgotPasswordApi({ email });
      setSent(true);
    } catch (requestError) {
      setError(requestError.message || 'The reset request could not be sent.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <div className="arcade-auth">
        <section className="arcade-auth__hero" aria-labelledby="forgot-brand-title">
          <div>
            <p className="arcade-kicker">Player recovery channel</p>
            <h1 id="forgot-brand-title" className="arcade-wordmark">Find your way <span>Back to the table</span></h1>
          </div>
          <p className="arcade-copy max-w-xl">We will send a single-use recovery link if the address belongs to a player.</p>
        </section>
        <ArcadePanel className="arcade-auth__form" aria-labelledby="forgot-title">
          <form className="arcade-form-stack" onSubmit={submit}>
            <div>
              <p className="arcade-eyebrow">Password recovery</p>
              <h2 id="forgot-title" className="arcade-title">Request reset</h2>
              <p className="arcade-copy mt-3">The response is the same for every address to protect player privacy.</p>
            </div>
            {sent ? (
              <StatusBanner tone="success" live>If an account exists for that email, a reset link is on its way.</StatusBanner>
            ) : (
              <>
                <FormInput type="email" label="Email" placeholder="player@example.com" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
                <ErrorPopup message={error} />
                <SubmitButton fullWidth loading={submitting}>Send reset link</SubmitButton>
              </>
            )}
            <p className="arcade-copy text-sm text-center"><Link to="/login">Return to login</Link></p>
          </form>
        </ArcadePanel>
      </div>
    </PageContainer>
  );
}
