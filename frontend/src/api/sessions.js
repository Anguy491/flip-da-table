const API_BASE = '/api';

async function post(path, payload, token) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
    credentials: 'include',
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data?.error || 'Request failed');
  return data;
}

export function createSession({ gameType, maxPlayers }, token) {
  return post('/sessions', { gameType, maxPlayers }, token); // { sessionId }
}
