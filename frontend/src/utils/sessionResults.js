export function sessionResultsKey(sessionId) {
  return `session-results-${sessionId}`;
}

export function readSessionResults(sessionId) {
  try {
    const current = sessionStorage.getItem(sessionResultsKey(sessionId));
    if (current) return JSON.parse(current);
    const legacyUno = sessionStorage.getItem(`uno-results-${sessionId}`);
    return legacyUno ? JSON.parse(legacyUno) : null;
  } catch {
    return null;
  }
}

export function recordSessionResult({ sessionId, gameType, totalRounds, playersMeta, result }) {
  const stored = readSessionResults(sessionId) || { gameType, totalRounds, results: [], playersMeta };
  stored.gameType = gameType;
  stored.totalRounds = totalRounds;
  stored.playersMeta = playersMeta;
  const existing = stored.results.find((entry) => entry.round === result.round);
  if (existing) Object.assign(existing, result);
  else stored.results.push(result);
  stored.results.sort((left, right) => left.round - right.round);
  sessionStorage.setItem(sessionResultsKey(sessionId), JSON.stringify(stored));
  return stored;
}
