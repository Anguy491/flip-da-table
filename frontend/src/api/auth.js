const API_BASE = '/api';

async function request(path, payload, { method = 'POST' } = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: payload === undefined ? {} : { 'Content-Type': 'application/json' },
    body: payload === undefined ? undefined : JSON.stringify(payload),
    credentials: 'include',
  });

  const data = res.status === 204 ? {} : await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg = data?.error || res.statusText || 'Request failed';
    throw new Error(msg);
  }
  return data; // { userId, email, nickname, token }
}

/**
 * @param {{ email: string, password: string, nickname: string }} params
 * @returns {Promise<{ userId:number, email:string, nickname:string, token:string }>}
 */
export function RegisterApi({ email, password, nickname }) {
  return request('/auth/register', { email, password, nickname });
}

/**
 * @param {{ email: string, password: string }} params
 * @returns {Promise<{ userId:number, email:string, nickname:string, token:string }>}
 */
export function LoginApi({ email, password }) {
  return request('/auth/login', { email, password });
}

export function GetAuthCapabilitiesApi() {
  return request('/auth/capabilities', undefined, { method: 'GET' });
}

export function ForgotPasswordApi({ email }) {
  return request('/auth/password/forgot', { email });
}

export function ResetPasswordApi({ token, newPassword }) {
  return request('/auth/password/reset', { token, newPassword });
}

export function ExchangeGoogleCodeApi({ code }) {
  return request('/auth/google/exchange', { code });
}

export function LinkGoogleAccountApi({ code, password }) {
  return request('/auth/google/link', { code, password });
}
