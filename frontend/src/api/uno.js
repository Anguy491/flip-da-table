const API_BASE = '/api';

async function send(path, { method = 'GET', body, token } = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  let data = null;
  try { data = await res.json(); } catch { data = {}; }
  if (!res.ok) throw new Error(data?.error || 'Request failed');
  return data;
}

export function getUnoView(gameId, viewerId, token) {
  return send(`/games/uno/${gameId}/view?viewerId=${viewerId}`, { token });
}

export function unoCommand(gameId, command, token) {
  return send(`/games/uno/${gameId}/commands`, { method: 'POST', body: command, token });
}
