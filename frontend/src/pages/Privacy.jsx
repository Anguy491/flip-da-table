import { Link } from 'react-router-dom';
import PageContainer from '../components/PageContainer';
import { ArcadePanel } from '../components/arcade/ArcadeUI';
import useAuthCapabilities from '../hooks/useAuthCapabilities';

export default function Privacy({ previewCapabilities = null }) {
  const capabilities = useAuthCapabilities(previewCapabilities);
  const supportEmail = capabilities.supportEmail || 'support@anguy.dev';
  return (
    <PageContainer>
      <ArcadePanel className="arcade-policy" aria-labelledby="privacy-title">
        <p className="arcade-eyebrow">Public information // last updated 23 August 2026</p>
        <h1 id="privacy-title" className="arcade-title">Privacy policy</h1>
        <p className="arcade-copy">Flip Da Table uses the minimum account information needed to provide multiplayer games and account recovery.</p>

        <section>
          <h2>Information we store</h2>
          <p className="arcade-copy">We store your email address, nickname, password hash, game and session records, and security metadata needed to protect your account. If you use Google sign-in, we also store Google&apos;s stable account identifier (<code>sub</code>) and the verified email supplied at linking time.</p>
        </section>
        <section>
          <h2>How information is used</h2>
          <p className="arcade-copy">Account information is used only to authenticate you, recover your account, display your nickname at tables, and operate game sessions. We do not sell personal information.</p>
        </section>
        <section>
          <h2>Service providers</h2>
          <p className="arcade-copy">Google Identity Services verifies optional Google sign-in. Resend delivers password-recovery email. Each provider processes the information required to perform that service under its own terms and privacy policy.</p>
        </section>
        <section>
          <h2>Retention and security</h2>
          <p className="arcade-copy">Passwords, recovery tokens, and sign-in handoff codes are not stored in readable form. Expired recovery and handoff records are deleted automatically. Account and game data is retained while the service is operating or until deletion is requested, except where retention is required for security or legal reasons.</p>
        </section>
        <section>
          <h2>Access and deletion</h2>
          <p className="arcade-copy">To ask about your data or request account deletion, contact <a href={`mailto:${supportEmail}`}>{supportEmail}</a>.</p>
        </section>
        <p className="arcade-copy"><Link to="/login">Return to Flip Da Table</Link></p>
      </ArcadePanel>
    </PageContainer>
  );
}
