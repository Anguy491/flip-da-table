import { useEffect, useRef, useState } from 'react';
import { StatusBanner } from './arcade/ArcadeUI';

const SCRIPT_ID = 'google-identity-services';
let scriptPromise;

function loadGoogleIdentityServices() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google);
  if (scriptPromise && !document.getElementById(SCRIPT_ID)) scriptPromise = undefined;
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise((resolve, reject) => {
    const existing = document.getElementById(SCRIPT_ID);
    const script = existing || document.createElement('script');
    const fail = (message) => {
      script.remove();
      reject(new Error(message));
    };
    const onLoad = () => window.google?.accounts?.id
      ? resolve(window.google)
      : fail('Google Identity Services unavailable');
    const onError = () => fail('Google Identity Services failed to load');
    script.addEventListener('load', onLoad, { once: true });
    script.addEventListener('error', onError, { once: true });
    if (!existing) {
      script.id = SCRIPT_ID;
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      document.head.appendChild(script);
    }
  }).catch((error) => {
    scriptPromise = undefined;
    throw error;
  });
  return scriptPromise;
}

export default function GoogleSignInButton({ capability }) {
  const targetRef = useRef(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!capability?.enabled || !capability.clientId || !capability.loginUri) return undefined;
    let active = true;
    loadGoogleIdentityServices()
      .then((google) => {
        if (!active || !targetRef.current) return;
        targetRef.current.replaceChildren();
        google.accounts.id.initialize({
          client_id: capability.clientId,
          login_uri: capability.loginUri,
          ux_mode: 'redirect',
          auto_select: false,
          cancel_on_tap_outside: true,
        });
        google.accounts.id.renderButton(targetRef.current, {
          type: 'standard',
          theme: 'filled_black',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          logo_alignment: 'left',
          width: Math.min(400, Math.max(240, targetRef.current.clientWidth)),
        });
      })
      .catch(() => { if (active) setError('Google sign-in is temporarily unavailable. You can still use email and password.'); });
    return () => { active = false; };
  }, [capability]);

  if (!capability?.enabled) return null;
  return (
    <div className="google-signin">
      <div className="arcade-auth-divider"><span>or</span></div>
      <div ref={targetRef} className="google-signin__slot" aria-label="Continue with Google" />
      {error && <StatusBanner tone="warning" live>{error}</StatusBanner>}
    </div>
  );
}
