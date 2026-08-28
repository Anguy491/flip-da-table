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

const conquerTemplates = [
  { id: 'T01', points: 1, lines: [{ id: 'L1', type: 'MILITARY', threshold: 5, symbols: [], display: 'Military ≥ 5' }] },
  { id: 'T02', points: 1, lines: [{ id: 'L1', type: 'MILITARY', threshold: 7, symbols: [], display: 'Military ≥ 7' }] },
  { id: 'T03', points: 1, lines: [{ id: 'L1', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'KNIGHT'], display: 'Raven + Knight' }] },
  { id: 'T04', points: 1, lines: [{ id: 'L1', type: 'SYMBOLS', threshold: null, symbols: ['CROWN', 'KNIGHT'], display: 'Crown + Knight' }] },
  { id: 'T05', points: 2, lines: [{ id: 'L1', type: 'MILITARY', threshold: 5, symbols: [], display: 'Military ≥ 5' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'KNIGHT'], display: 'Raven + Knight' }] },
  { id: 'T06', points: 2, lines: [{ id: 'L1', type: 'MILITARY', threshold: 3, symbols: [], display: 'Military ≥ 3' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'RAVEN'], display: 'Raven + Raven' }] },
  { id: 'T07', points: 2, lines: [{ id: 'L1', type: 'MILITARY', threshold: 3, symbols: [], display: 'Military ≥ 3' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['KNIGHT', 'KNIGHT'], display: 'Knight + Knight' }] },
  { id: 'T08', points: 2, lines: [{ id: 'L1', type: 'MILITARY', threshold: 8, symbols: [], display: 'Military ≥ 8' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['CROWN'], display: 'Crown' }] },
  { id: 'T09', points: 2, lines: [{ id: 'L1', type: 'MILITARY', threshold: 2, symbols: [], display: 'Military ≥ 2' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'RAVEN'], display: 'Raven + Raven' }, { id: 'L3', type: 'SYMBOLS', threshold: null, symbols: ['KNIGHT'], display: 'Knight' }] },
  { id: 'T10', points: 2, lines: [{ id: 'L1', type: 'MILITARY', threshold: 4, symbols: [], display: 'Military ≥ 4' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'KNIGHT'], display: 'Raven + Knight' }, { id: 'L3', type: 'SYMBOLS', threshold: null, symbols: ['CROWN'], display: 'Crown' }] },
  { id: 'T11', points: 3, lines: [{ id: 'L1', type: 'MILITARY', threshold: 6, symbols: [], display: 'Military ≥ 6' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'KNIGHT'], display: 'Raven + Knight' }] },
  { id: 'T12', points: 3, lines: [{ id: 'L1', type: 'MILITARY', threshold: 6, symbols: [], display: 'Military ≥ 6' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'RAVEN'], display: 'Raven + Raven' }] },
  { id: 'T13', points: 3, lines: [{ id: 'L1', type: 'MILITARY', threshold: 5, symbols: [], display: 'Military ≥ 5' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'RAVEN'], display: 'Raven + Raven' }, { id: 'L3', type: 'SYMBOLS', threshold: null, symbols: ['KNIGHT'], display: 'Knight' }] },
  { id: 'T14', points: 4, lines: [{ id: 'L1', type: 'MILITARY', threshold: 6, symbols: [], display: 'Military ≥ 6' }, { id: 'L2', type: 'SYMBOLS', threshold: null, symbols: ['RAVEN', 'RAVEN'], display: 'Raven + Raven' }, { id: 'L3', type: 'SYMBOLS', threshold: null, symbols: ['KNIGHT'], display: 'Knight' }] },
];

const conquerNames = [
  ['White Harbor', 'Stark–Tully Alliance'], ['Moat Cailin', 'Stark–Tully Alliance'],
  ['Harrenhal', 'Lannister Royalists'], ['Ten Towers', 'Greyjoy'], ['Highgarden', 'Tyrell'],
  ['Riverrun', 'Stark–Tully Alliance'], ['Pyke', 'Greyjoy'], ['Dragonstone', 'Baratheon'],
  ['Oldtown', 'Tyrell'], ["King's Landing", 'Lannister Royalists'], ['Winterfell', 'Stark–Tully Alliance'],
  ['Casterly Rock', 'Lannister Royalists'], ['The Eyrie', 'Arryn'], ["Storm's End", 'Baratheon'],
];

const conquerDanceNames = [
  ['The Eyrie', 'Blacks · Targaryen'], ['Maidenpool', 'Blacks · Targaryen'],
  ["Storm's End", 'Greens · Targaryen'], ['Lannisport', 'Lannister'], ['Winterfell', 'Stark'],
  ['Harrenhal', 'Blacks · Targaryen'], ['Casterly Rock', 'Lannister'], ['Driftmark', 'Velaryon'],
  ['White Harbor', 'Stark'], ['Oldtown', 'Greens · Targaryen'], ['Dragonstone', 'Blacks · Targaryen'],
  ["King's Landing", 'Greens · Targaryen'], ['Riverrun', 'Tully'], ['High Tide', 'Velaryon'],
];

const conquerUsurperNames = [
  ['Stoney Sept', 'Stark–Arryn–Tully Alliance'], ['The Eyrie', 'Stark–Arryn–Tully Alliance'],
  ['Tower of Joy', 'Targaryen Royalists'], ['Lannisport', 'Lannister'], ['Ashford', 'Tyrell Royalists'],
  ['Riverrun', 'Stark–Arryn–Tully Alliance'], ['Casterly Rock', 'Lannister'], ['Ruby Ford', 'Baratheon Rebels'],
  ['Highgarden', 'Tyrell Royalists'], ["King's Landing", 'Targaryen Royalists'], ['Winterfell', 'Stark–Arryn–Tully Alliance'],
  ['Dragonstone', 'Targaryen Royalists'], ['Sunspear', 'Martell'], ["Storm's End", 'Baratheon Rebels'],
];

const conquerAegonNames = [
  ['Maidenpool', 'Hoare · Isles and Rivers'], ['Pyke', 'Hoare · Isles and Rivers'],
  ['Oldtown', 'Targaryen Conquerors'], ['Gulltown', 'Arryn · Mountain and Vale'],
  ['Field of Fire', 'Gardener–Lannister Alliance'], ['Riverrun', 'Hoare · Isles and Rivers'],
  ['The Eyrie', 'Arryn · Mountain and Vale'], ['Last Storm', 'Durrandon Storm Kingdom'],
  ['Highgarden', 'Gardener–Lannister Alliance'], ['Aegonfort', 'Targaryen Conquerors'],
  ['Harrenhal', 'Hoare · Isles and Rivers'], ['Dragonstone', 'Targaryen Conquerors'],
  ['Winterfell', 'Stark · Kingdom of the North'], ["Storm's End", 'Durrandon Storm Kingdom'],
];

export const conquerWesterosFixture = {
  schemaVersion: 2,
  phase: 'WAITING_FOR_DECISION',
  stateVersion: 8,
  campaign: 'WAR_OF_FIVE_KINGS',
  campaignName: 'War of the Five Kings',
  turnCount: 3,
  viewerId: 'P1',
  currentPlayerId: 'P1',
  ironThroneHolderId: 'P2',
  currentRoll: [
    { dieId: 0, face: 'MILITARY_3', militaryStrength: 3, display: 'Military 3' },
    { dieId: 1, face: 'MILITARY_2', militaryStrength: 2, display: 'Military 2' },
    { dieId: 2, face: 'RAVEN', militaryStrength: 0, display: 'Raven' },
    { dieId: 3, face: 'KNIGHT', militaryStrength: 0, display: 'Knight' },
    { dieId: 4, face: 'CROWN', militaryStrength: 0, display: 'Crown' },
    { dieId: 5, face: 'RAVEN', militaryStrength: 0, display: 'Raven' },
    { dieId: 6, face: 'MILITARY_1', militaryStrength: 1, display: 'Military 1' },
  ],
  attempt: { targetId: null, targetOwnerId: null, stealing: false, completedLineIds: [], lostDieIds: [], committedDieIds: [], requiredLines: [] },
  players: [
    { playerId: 'P1', name: 'PixelPilot', bot: false, seatIndex: 0, current: true, holdsThrone: false, faceUpStrongholds: [], completedClans: [], strongholdCount: 0, completedClanCount: 0, faceUpScore: 0, clanScore: 0, totalScore: 0 },
    { playerId: 'P2', name: 'CipherFox', bot: false, seatIndex: 1, current: false, holdsThrone: true, faceUpStrongholds: ['T10'], completedClans: [], strongholdCount: 1, completedClanCount: 0, faceUpScore: 2, clanScore: 0, totalScore: 3 },
    { playerId: 'BOT1', name: 'Bot 1', bot: true, seatIndex: 2, current: false, holdsThrone: false, faceUpStrongholds: [], completedClans: [], strongholdCount: 0, completedClanCount: 0, faceUpScore: 0, clanScore: 0, totalScore: 0 },
  ],
  strongholds: conquerTemplates.map((template, index) => ({
    ...template,
    name: conquerNames[index][0],
    clan: conquerNames[index][1],
    kingsLanding: template.id === 'T10',
    ownerId: template.id === 'T10' ? 'P2' : null,
    central: template.id !== 'T10',
    locked: false,
    stealCrownRequired: false,
    lines: template.lines.map((line) => ({ ...line, completed: false, special: false })),
  })),
  legalActions: { canRoll: false, canCompleteLine: true, canLoseDie: true, legalTargetIds: conquerTemplates.map((item) => item.id), legalDieIds: [0, 1, 2, 3, 4, 5, 6] },
  events: [
    { sequence: 1, type: 'GAME_STARTED', actorId: null, targetId: null, text: 'War of the Five Kings began' },
    { sequence: 7, type: 'TURN_STARTED', actorId: 'P1', targetId: null, text: 'PixelPilot is ready to roll' },
    { sequence: 8, type: 'ROLL_DICE', actorId: 'P1', targetId: null, text: 'PixelPilot rolled 7 dice' },
  ],
  results: [],
};

export const conquerWesterosDanceFixture = {
  ...conquerWesterosFixture,
  campaign: 'DANCE_OF_THE_DRAGONS',
  campaignName: 'Dance of the Dragons',
  players: conquerWesterosFixture.players.map((player) => player.playerId === 'P2'
    ? { ...player, faceUpStrongholds: ['T12'] }
    : player),
  strongholds: conquerTemplates.map((template, index) => ({
    ...template,
    name: conquerDanceNames[index][0],
    clan: conquerDanceNames[index][1],
    kingsLanding: template.id === 'T12',
    ownerId: template.id === 'T12' ? 'P2' : null,
    central: template.id !== 'T12',
    locked: false,
    stealCrownRequired: false,
    lines: template.lines.map((line) => ({ ...line, completed: false, special: false })),
  })),
  events: [
    { sequence: 1, type: 'GAME_STARTED', actorId: null, targetId: null, text: 'Dance of the Dragons began' },
    { sequence: 7, type: 'TURN_STARTED', actorId: 'P1', targetId: null, text: 'PixelPilot is ready to roll' },
    { sequence: 8, type: 'ROLL_DICE', actorId: 'P1', targetId: null, text: 'PixelPilot rolled 7 dice' },
  ],
};

export const conquerWesterosUsurperFixture = {
  ...conquerWesterosFixture,
  campaign: 'WAR_OF_THE_USURPER',
  campaignName: 'War of the Usurper',
  strongholds: conquerTemplates.map((template, index) => ({
    ...template,
    name: conquerUsurperNames[index][0],
    clan: conquerUsurperNames[index][1],
    kingsLanding: template.id === 'T10',
    ownerId: template.id === 'T10' ? 'P2' : null,
    central: template.id !== 'T10',
    locked: false,
    stealCrownRequired: false,
    lines: template.lines.map((line) => ({ ...line, completed: false, special: false })),
  })),
  events: [
    { sequence: 1, type: 'GAME_STARTED', actorId: null, targetId: null, text: 'War of the Usurper began' },
    { sequence: 7, type: 'TURN_STARTED', actorId: 'P1', targetId: null, text: 'PixelPilot is ready to roll' },
    { sequence: 8, type: 'ROLL_DICE', actorId: 'P1', targetId: null, text: 'PixelPilot rolled 7 dice' },
  ],
};

export const conquerWesterosAegonFixture = {
  ...conquerWesterosFixture,
  campaign: 'AEGONS_CONQUEST',
  campaignName: "Aegon's Conquest",
  strongholds: conquerTemplates.map((template, index) => ({
    ...template,
    name: conquerAegonNames[index][0],
    clan: conquerAegonNames[index][1],
    kingsLanding: template.id === 'T10',
    ownerId: template.id === 'T10' ? 'P2' : null,
    central: template.id !== 'T10',
    locked: false,
    stealCrownRequired: false,
    lines: template.lines.map((line) => ({ ...line, completed: false, special: false })),
  })),
  events: [
    { sequence: 1, type: 'GAME_STARTED', actorId: null, targetId: null, text: "Aegon's Conquest began" },
    { sequence: 7, type: 'TURN_STARTED', actorId: 'P1', targetId: null, text: 'PixelPilot is ready to roll' },
    { sequence: 8, type: 'ROLL_DICE', actorId: 'P1', targetId: null, text: 'PixelPilot rolled 7 dice' },
  ],
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
