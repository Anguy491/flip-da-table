const API_BASE = '/api';

async function get(path, token) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    credentials: 'include',
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data?.error || 'Request failed');
  return data;
}

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

export function getSession(sessionId, token) {
  return get(`/sessions/${sessionId}`, token); // { id, ownerId, gameType, maxPlayers }
}

export function startFirstGame(sessionId, payload, token) {
  // Returns { gameId, roundIndex, myPlayerId, players: [{playerId,name,bot,ready}], view? }
  return post(`/sessions/${sessionId}/start`, payload, token);
}

export function startNextGame(sessionId, payload, token) {
  return post(`/sessions/${sessionId}/start/next`, payload, token);
}