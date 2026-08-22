export function readAuthFragment(hash = window.location.hash) {
  const params = new URLSearchParams(hash.replace(/^#/, ''));
  return {
    code: params.get('code') || '',
    link: params.get('link') || '',
    googleError: params.get('google_error') || '',
    resetToken: params.get('token') || '',
  };
}

export function clearAuthFragment() {
  if (window.location.hash) {
    window.history.replaceState(window.history.state, '', `${window.location.pathname}${window.location.search}`);
  }
}
