export function isArrangementValid(cards = []) {
  let previousValue = -1;
  let previousColor = null;
  for (const card of cards) {
    if (card?.isJoker) continue;
    const value = typeof card?.value === 'number' ? card.value : Number.parseInt(card?.value, 10);
    if (Number.isNaN(value)) continue;
    if (value < previousValue) return false;
    if (value === previousValue && previousColor === 'WHITE' && card?.color === 'BLACK') return false;
    previousValue = value;
    previousColor = card?.color;
  }
  return true;
}
