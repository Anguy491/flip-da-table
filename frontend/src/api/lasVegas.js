const API_BASE = '/api/games/las-vegas';

async function request(path, { method = 'GET', body, token } = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data?.error || 'Request failed');
    error.status = response.status;
    throw error;
  }
  return data;
}

export function getLasVegasView(gameId, token) {
  return request(`/${gameId}/view`, { token });
}

export function sendLasVegasCommand(gameId, command, token) {
  return request(`/${gameId}/commands`, { method: 'POST', body: command, token });
}

export function setLasVegasAssetVisibility(gameId, visible, token) {
  return request(`/${gameId}/presentation/assets`, { method: 'POST', body: { visible }, token });
}
