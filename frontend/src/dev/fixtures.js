export const unoFixture = {
  sessionId: 'ARCADE-8BIT-2026',
  gameId: 'UNO-DEMO-01',
  players: Array.from({ length: 10 }, (_, index) => ({
    id: `player-${index + 1}`,
    name: index === 3 ? 'LongNicknameThatNeedsTruncation' : index === 0 ? 'PixelPilot' : `Player ${index + 1}`,
    handCount: 3 + index,
  })),
  currentPlayerId: 'player-1',
  round: 3,
  direction: 'CW',
  activeColor: 'RED',
  pendingDraw: 2,
  topCard: { id: 'top', color: 'RED', value: 'DRAW_TWO' },
  events: [
    { id: 1, text: 'Player 4 changed the active color to red.' },
    { id: 2, text: 'Player 8 played DRAW TWO.' },
    { id: 3, text: 'PixelPilot is choosing a response.' },
  ],
  hand: [
    { id: 'u1', color: 'RED', value: '7' },
    { id: 'u2', color: 'BLUE', value: 'REVERSE' },
    { id: 'u3', color: 'GREEN', value: '3' },
    { id: 'u4', color: 'YELLOW', value: 'SKIP' },
    { id: 'u5', color: 'NONE', value: 'WILD' },
    { id: 'u6', color: 'NONE', value: 'WILD_DRAW_FOUR' },
  ],
};

export const dvcFixture = {
  sessionId: 'CODE-ROOM-2048',
  gameId: 'DVC-DEMO-01',
  myPlayerId: 'PixelPilot',
  currentPlayerId: 'CipherFox',
  roundIndex: 2,
  awaiting: 'GUESS_SELECTION',
  board: {
    awaiting: 'GUESS_SELECTION',
    currentPlayerIndex: 1,
    deckRemaining: 14,
    deckBlackRemaining: 7,
    deckWhiteRemaining: 7,
    turnId: 9,
  },
  playerViews: [
    { playerId: 'PixelPilot', hiddenCount: 4, cards: ['BLACK 1', 'WHITE 3', 'BLACK 5', 'WHITE 8'] },
    { playerId: 'CipherFox', hiddenCount: 3, cards: ['BLACK ≤', 'WHITE 4', 'WHITE ≤', 'BLACK ≤'] },
    { playerId: 'LongNicknameThatNeedsTruncation', hiddenCount: 4, cards: ['WHITE ≤', 'BLACK ≤', 'WHITE ≤', 'BLACK ≤'] },
    { playerId: 'Bot 3', hiddenCount: 2, cards: ['BLACK 0', 'WHITE ≤', 'BLACK ≤', 'WHITE 11'] },
  ],
  myCards: [
    { color: 'BLACK', value: 1, revealed: true, isJoker: false },
    { color: 'WHITE', value: 3, revealed: true, isJoker: false },
    { color: 'BLACK', value: 5, revealed: true, isJoker: false },
    { color: 'WHITE', value: 8, revealed: true, isJoker: false },
  ],
  actionLog: [
    { seq: 1, type: 'DRAW', correct: null, text: 'PixelPilot chose the BLACK pile and drew a tile.' },
    { seq: 2, type: 'GUESS', correct: false, text: "PixelPilot guessed CipherFox's #3 WHITE tile as 6 — WRONG." },
    { seq: 3, type: 'DRAW', correct: null, text: 'CipherFox chose the WHITE pile and drew a tile.' },
    { seq: 4, type: 'GUESS', correct: true, text: "CipherFox guessed PixelPilot's #2 WHITE tile as 3 — CORRECT." },
    { seq: 5, type: 'DECISION', correct: null, text: 'CipherFox ended the guess run.' },
  ],
};

export const lasVegasFixture = {
  schemaVersion: 2,
  phase: 'WAITING_FOR_CHOICE',
  stateVersion: 12,
  internalRound: 2,
  totalRounds: 3,
  turnCount: 8,
  viewerId: 'P1',
  currentPlayerId: 'P1',
  currentRoll: [
    { face: 1, big: false },
    { face: 1, big: true },
    { face: 3, big: false },
    { face: 5, big: false },
  ],
  players: Array.from({ length: 10 }, (_, index) => ({
    playerId: index === 9 ? 'BOT1' : `P${index + 1}`,
    name: index === 9 ? 'Bot 1' : index === 3 ? 'LongNicknameThatNeedsTruncation' : index === 0 ? 'PixelPilot' : `Player ${index + 1}`,
    bot: index === 9,
    seatIndex: index,
    current: index === 0,
    remainingRegularDice: index === 9 ? 3 : Math.max(0, 6 - index),
    bigDieRemaining: index === 9 || index % 3 !== 0,
    remainingDice: index === 9 ? 4 : Math.max(0, 6 - index) + (index % 3 !== 0 ? 1 : 0),
    chips: 2 + (index % 4),
    moneyCardCount: index % 5,
    moneyCards: index === 0 ? [80_000, 50_000] : null,
    cashTotal: index === 0 ? 130_000 : null,
    totalAssets: index === 0 ? 150_000 : null,
    presentedTotal: index === 4 ? 210_000 : null,
  })),
  casinos: Array.from({ length: 6 }, (_, index) => ({
    number: index + 1,
    bonuses: [100_000 - index * 10_000, 70_000 - index * 5_000],
    placements: [
      { playerId: `P${(index % 10) + 1}`, regularDice: 2, bigDie: false, influence: 2 },
      { playerId: `P${((index + 2) % 10) + 1}`, regularDice: 1, bigDie: true, influence: 3 },
    ],
  })),
  events: [
    { sequence: 9, type: 'PLACE_DICE', actorId: 'P4', casinoNumber: 6, text: 'Player 4 placed dice at casino 6' },
    { sequence: 10, type: 'CASINO_JACKPOT', actorId: 'P5', casinoNumber: 6, text: 'Player 5 won casino 6 jackpot' },
    { sequence: 11, type: 'ROUND_STARTED', actorId: 'P1', casinoNumber: null, text: 'Casino round 2 started' },
  ],
  results: [],
};

export const dashboardFixture = {
  me: { nickname: 'PixelPilot' },
};

export const lobbyFixture = {
  sessionId: 'ARCADE-ROOM-8BIT-2048',
  myUserId: 'host-1',
  rounds: 3,
  connectionState: 'connected',
  sessionInfo: {
    gameType: 'UNO',
    maxPlayers: 10,
    ownerId: 'host-1',
    capabilities: { minPlayers: 2, maxPlayers: 10, botsAllowed: true, seriesAllowed: true, internalRounds: 1 },
  },
  players: Array.from({ length: 10 }, (_, index) => ({
    name: index === 0 ? 'PixelPilot' : index === 3 ? 'LongNicknameThatNeedsTruncation' : index > 7 ? `Bot ${index - 7}` : `Player ${index + 1}`,
    bot: index > 7,
    ready: index !== 6,
  })),
};

export const summaryFixture = {
  totalRounds: 6,
  playersMeta: [
    { playerId: 'p1', name: 'PixelPilot' },
    { playerId: 'p2', name: 'CipherFox' },
    { playerId: 'p3', name: 'LongNicknameThatNeedsTruncation' },
    { playerId: 'p4', name: 'Bot 1' },
    { playerId: 'p5', name: 'Player 5' },
  ],
  results: [
    { round: 1, winnerId: 'p1', winnerName: 'PixelPilot', turns: 19 },
    { round: 2, winnerId: 'p2', winnerName: 'CipherFox', turns: 23 },
    { round: 3, winnerId: 'p3', winnerName: 'LongNicknameThatNeedsTruncation', turns: 17 },
    { round: 4, winnerId: 'p1', winnerName: 'PixelPilot', turns: 28 },
    { round: 5, winnerId: 'p2', winnerName: 'CipherFox', turns: 21 },
    { round: 6, winnerId: 'p4', winnerName: 'Bot 1', turns: 15 },
  ],
};
