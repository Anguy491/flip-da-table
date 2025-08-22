const API_BASE = '/api';

async function request(path, payload) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    credentials: 'include',
  });

  const data = await res.json().catch(() => ({}));
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
