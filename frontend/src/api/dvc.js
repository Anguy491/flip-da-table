// DVC API client
const base = '';

async function jsonFetch(url, opts = {}) {
  const res = await fetch(url, { headers: { 'Content-Type': 'application/json', ...(opts.headers||{}) }, ...opts });
  if (!res.ok) throw new Error(await res.text() || res.statusText);
  if (res.status === 204) return null;
  return res.json();
}

export async function fetchDvcView(gameId, playerId, token) {
  return jsonFetch(`/api/dvc/${encodeURIComponent(gameId)}/view/${encodeURIComponent(playerId)}`, { headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function drawColor(gameId, playerId, color, token) {
  return jsonFetch(`/api/dvc/${encodeURIComponent(gameId)}/drawColor`, { method: 'POST', body: JSON.stringify({ playerId, color }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function guess(gameId, playerId, targetPlayerId, targetIndex, joker, number, token) {
  return jsonFetch(`/api/dvc/${encodeURIComponent(gameId)}/guess`, { method: 'POST', body: JSON.stringify({ playerId, targetPlayerId, targetIndex, joker, number }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function revealDecision(gameId, playerId, cont, token) {
  return jsonFetch(`/api/dvc/${encodeURIComponent(gameId)}/revealDecision`, { method: 'POST', body: JSON.stringify({ playerId, cont }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function selfReveal(gameId, playerId, ownIndex, token) {
  return jsonFetch(`/api/dvc/${encodeURIComponent(gameId)}/selfReveal`, { method: 'POST', body: JSON.stringify({ playerId, ownIndex }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}

export async function settle(gameId, playerId, insertIndex, token) {
  return jsonFetch(`/api/dvc/${encodeURIComponent(gameId)}/settle`, { method: 'POST', body: JSON.stringify({ playerId, insertIndex }), headers: token? { Authorization: `Bearer ${token}` }: {} });
}
