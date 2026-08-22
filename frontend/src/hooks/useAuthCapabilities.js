import { useEffect, useState } from 'react';
import { GetAuthCapabilitiesApi } from '../api/auth';

const disabledCapabilities = {
  passwordReset: false,
  supportEmail: 'support@anguy.dev',
  google: { enabled: false, clientId: '', loginUri: '' },
};

export default function useAuthCapabilities(previewCapabilities = null) {
  const [capabilities, setCapabilities] = useState(previewCapabilities || disabledCapabilities);

  useEffect(() => {
    if (previewCapabilities) {
      setCapabilities(previewCapabilities);
      return undefined;
    }
    let alive = true;
    GetAuthCapabilitiesApi()
      .then((value) => { if (alive) setCapabilities(value); })
      .catch(() => { if (alive) setCapabilities(disabledCapabilities); });
    return () => { alive = false; };
  }, [previewCapabilities]);

  return capabilities;
}
