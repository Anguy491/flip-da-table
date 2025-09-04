// DVC API client
const base = '';

async function jsonFetch(url, opts = {}) {
  const baseHeaders = { 'Content-Type': 'application/json' };
  const mergedHeaders = { ...baseHeaders, ...(opts.headers || {}) };
  const { headers: _ignored, ...rest } = opts;
  const res = await fetch(url, {
    ...rest,
    headers: mergedHeaders,
    credentials: 'include'
  });
  let data = null;
  try { data = await res.json(); } catch { /* ignore parse */ }
  if (!res.ok) throw new Error(data?.error || 'Request failed');
  return data;
}

export async function fetchDvcView(gameId, playerId, token) {
  return jsonFetch(`/api/dvc/${gameId}/view/${playerId}` , { headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function drawColor(gameId, playerId, color, token) {
  return jsonFetch(`/api/dvc/${gameId}/drawColor`, { method: 'POST', body: JSON.stringify({ playerId, color }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function guess(gameId, playerId, targetPlayerId, targetIndex, joker, number, token) {
  return jsonFetch(`/api/dvc/${gameId}/guess`, { method: 'POST', body: JSON.stringify({ playerId, targetPlayerId, targetIndex, joker, number }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function revealDecision(gameId, playerId, cont, token) {
  return jsonFetch(`/api/dvc/${gameId}/revealDecision`, { method: 'POST', body: JSON.stringify({ playerId, cont }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function selfReveal(gameId, playerId, ownIndex, token) {
  return jsonFetch(`/api/dvc/${gameId}/selfReveal`, { method: 'POST', body: JSON.stringify({ playerId, ownIndex }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function settle(gameId, playerId, hand, isSettled=true, token) {
  return jsonFetch(`/api/dvc/${gameId}/settle`, { method: 'POST', body: JSON.stringify({ playerId, isSettled, hand }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}
