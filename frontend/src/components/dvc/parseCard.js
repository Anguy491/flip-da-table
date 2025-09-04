export function parseCard(str) {
  if (!str || typeof str !== 'string') return { color: 'BLACK', value: null, revealed: false, isJoker: false };
  const parts = str.split(/\s+/);
  const color = parts[0];
  const raw = parts[1];
  if (raw === '≤') return { color, value: null, revealed: false, isJoker: false };
  if (raw === '-') return { color, value: '-', revealed: true, isJoker: true };
  return { color, value: raw, revealed: true, isJoker: false };
}
