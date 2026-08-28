/* eslint-disable jsx-a11y/no-noninteractive-tabindex -- The scrollable tactical map needs a keyboard target. */
const PLACE_COORDINATES = Object.freeze({
  WINTERFELL: { x: 43, y: 20 },
  WHITE_HARBOR: { x: 59, y: 30 },
  MOAT_CAILIN: { x: 44, y: 38 },
  TEN_TOWERS: { x: 23, y: 45 },
  PYKE: { x: 14, y: 51 },
  THE_EYRIE: { x: 70, y: 48 },
  RIVERRUN: { x: 39, y: 51 },
  HARRENHAL: { x: 52, y: 55 },
  MAIDENPOOL: { x: 61, y: 54 },
  CASTERLY_ROCK: { x: 29, y: 58 },
  LANNISPORT: { x: 20, y: 65 },
  KINGS_LANDING: { x: 57, y: 63 },
  DRIFTMARK: { x: 68, y: 60 },
  HIGH_TIDE: { x: 78, y: 54 },
  DRAGONSTONE: { x: 82, y: 64 },
  HIGHGARDEN: { x: 39, y: 72 },
  STORMS_END: { x: 66, y: 72 },
  OLDTOWN: { x: 29, y: 83 },
  STONEY_SEPT: { x: 44, y: 59 },
  RUBY_FORD: { x: 55, y: 53 },
  ASHFORD: { x: 49, y: 77 },
  TOWER_OF_JOY: { x: 45, y: 86 },
  SUNSPEAR: { x: 56, y: 90 },
  GULLTOWN: { x: 82, y: 48 },
  FIELD_OF_FIRE: { x: 39, y: 64 },
  LAST_STORM: { x: 49, y: 70 },
  MAIDENPOOL_CONQUEST: { x: 67, y: 56 },
});

const MAP_LAYOUTS = Object.freeze({
  WAR_OF_FIVE_KINGS: Object.freeze({
    T01: PLACE_COORDINATES.WHITE_HARBOR,
    T02: PLACE_COORDINATES.MOAT_CAILIN,
    T03: PLACE_COORDINATES.HARRENHAL,
    T04: PLACE_COORDINATES.TEN_TOWERS,
    T05: PLACE_COORDINATES.HIGHGARDEN,
    T06: PLACE_COORDINATES.RIVERRUN,
    T07: PLACE_COORDINATES.PYKE,
    T08: PLACE_COORDINATES.DRAGONSTONE,
    T09: PLACE_COORDINATES.OLDTOWN,
    T10: PLACE_COORDINATES.KINGS_LANDING,
    T11: PLACE_COORDINATES.WINTERFELL,
    T12: PLACE_COORDINATES.CASTERLY_ROCK,
    T13: PLACE_COORDINATES.THE_EYRIE,
    T14: PLACE_COORDINATES.STORMS_END,
  }),
  DANCE_OF_THE_DRAGONS: Object.freeze({
    T01: PLACE_COORDINATES.THE_EYRIE,
    T02: PLACE_COORDINATES.MAIDENPOOL,
    T03: PLACE_COORDINATES.STORMS_END,
    T04: PLACE_COORDINATES.LANNISPORT,
    T05: PLACE_COORDINATES.WINTERFELL,
    T06: PLACE_COORDINATES.HARRENHAL,
    T07: PLACE_COORDINATES.CASTERLY_ROCK,
    T08: PLACE_COORDINATES.DRIFTMARK,
    T09: PLACE_COORDINATES.WHITE_HARBOR,
    T10: PLACE_COORDINATES.OLDTOWN,
    T11: PLACE_COORDINATES.DRAGONSTONE,
    T12: PLACE_COORDINATES.KINGS_LANDING,
    T13: PLACE_COORDINATES.RIVERRUN,
    T14: PLACE_COORDINATES.HIGH_TIDE,
  }),
  WAR_OF_THE_USURPER: Object.freeze({
    T01: PLACE_COORDINATES.STONEY_SEPT,
    T02: PLACE_COORDINATES.THE_EYRIE,
    T03: PLACE_COORDINATES.TOWER_OF_JOY,
    T04: PLACE_COORDINATES.LANNISPORT,
    T05: PLACE_COORDINATES.ASHFORD,
    T06: PLACE_COORDINATES.RIVERRUN,
    T07: PLACE_COORDINATES.CASTERLY_ROCK,
    T08: PLACE_COORDINATES.RUBY_FORD,
    T09: PLACE_COORDINATES.HIGHGARDEN,
    T10: PLACE_COORDINATES.KINGS_LANDING,
    T11: PLACE_COORDINATES.WINTERFELL,
    T12: PLACE_COORDINATES.DRAGONSTONE,
    T13: PLACE_COORDINATES.SUNSPEAR,
    T14: PLACE_COORDINATES.STORMS_END,
  }),
  AEGONS_CONQUEST: Object.freeze({
    T01: PLACE_COORDINATES.MAIDENPOOL_CONQUEST,
    T02: PLACE_COORDINATES.PYKE,
    T03: PLACE_COORDINATES.OLDTOWN,
    T04: PLACE_COORDINATES.GULLTOWN,
    T05: PLACE_COORDINATES.FIELD_OF_FIRE,
    T06: PLACE_COORDINATES.RIVERRUN,
    T07: PLACE_COORDINATES.THE_EYRIE,
    T08: PLACE_COORDINATES.LAST_STORM,
    T09: PLACE_COORDINATES.HIGHGARDEN,
    T10: PLACE_COORDINATES.KINGS_LANDING,
    T11: PLACE_COORDINATES.HARRENHAL,
    T12: PLACE_COORDINATES.DRAGONSTONE,
    T13: PLACE_COORDINATES.WINTERFELL,
    T14: PLACE_COORDINATES.STORMS_END,
  }),
});

function WesterosMapArt() {
  return (
    <svg
      className="cw-map-art"
      viewBox="0 0 720 1000"
      preserveAspectRatio="xMidYMid meet"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <pattern id="cw-map-pixel-grid" width="24" height="24" patternUnits="userSpaceOnUse">
          <path className="cw-map-art__grid-line" d="M 24 0 L 0 0 0 24" />
        </pattern>
        <clipPath id="cw-map-mainland-clip">
          <path d="M310 42 360 28 417 43 463 78 448 112 480 149 459 190 493 228 476 274 514 316 486 359 518 399 495 444 531 486 512 528 546 566 517 609 506 651 536 691 501 730 478 770 456 804 463 839 421 871 405 916 360 957 316 943 286 920 249 931 214 901 205 861 179 830 190 790 161 755 184 713 174 675 201 638 190 598 216 561 200 520 225 481 214 440 241 399 231 359 260 328 249 289 278 258 264 220 291 184 270 149 300 114 282 82Z" />
        </clipPath>
      </defs>

      <rect className="cw-map-art__sea" width="720" height="1000" />
      <rect className="cw-map-art__grid" width="720" height="1000" fill="url(#cw-map-pixel-grid)" />

      <g className="cw-map-art__islands">
        <path d="M91 453 111 437 125 449 120 474 101 481 87 468Z" />
        <path d="M130 489 151 475 166 487 158 512 139 519 125 507Z" />
        <path d="M96 523 112 510 124 520 118 542 101 547 89 537Z" />
        <path d="M546 578 563 565 577 577 569 596 550 601 540 590Z" />
        <path d="M588 616 611 597 628 613 619 639 594 644 580 632Z" />
        <path d="M584 669 599 659 610 668 604 682 588 686 578 677Z" />
      </g>

      <path
        className="cw-map-art__land"
        d="M310 42 360 28 417 43 463 78 448 112 480 149 459 190 493 228 476 274 514 316 486 359 518 399 495 444 531 486 512 528 546 566 517 609 506 651 536 691 501 730 478 770 456 804 463 839 421 871 405 916 360 957 316 943 286 920 249 931 214 901 205 861 179 830 190 790 161 755 184 713 174 675 201 638 190 598 216 561 200 520 225 481 214 440 241 399 231 359 260 328 249 289 278 258 264 220 291 184 270 149 300 114 282 82Z"
      />

      <g clipPath="url(#cw-map-mainland-clip)">
        <path className="cw-map-art__region cw-map-art__region--north" d="M210 32H520V374L458 407 365 380 278 406 194 352Z" />
        <path className="cw-map-art__region cw-map-art__region--vale" d="M454 358 548 382 564 568 496 596 432 525Z" />
        <path className="cw-map-art__region cw-map-art__region--west" d="M142 402 312 392 358 566 292 672 153 638Z" />
        <path className="cw-map-art__region cw-map-art__region--crown" d="M310 408 468 398 526 612 444 687 306 614Z" />
        <path className="cw-map-art__region cw-map-art__region--reach" d="M146 616 364 588 449 728 386 836 208 829 137 749Z" />
        <path className="cw-map-art__region cw-map-art__region--storm" d="M420 600 550 603 549 758 450 796 393 714Z" />
        <path className="cw-map-art__region cw-map-art__region--south" d="M183 794 476 765 488 976 178 980Z" />
        <path className="cw-map-art__texture" d="M170 166 510 310M154 278 532 426M135 455 551 596M123 630 520 760M151 800 465 929" />
      </g>

      <g className="cw-map-art__boundaries">
        <path d="M251 370 365 380 458 407 495 444" />
        <path d="M218 489 310 510 432 525 517 514" />
        <path d="M198 626 306 614 444 687 520 650" />
        <path d="M184 713 330 704 450 796" />
        <path d="M188 822 386 836 456 804" />
      </g>

      <g className="cw-map-art__rivers">
        <path d="M336 354 354 417 338 468 372 529 353 590" />
        <path d="M440 485 411 534 423 596 391 652" />
        <path d="M287 608 320 650 296 704 327 759" />
      </g>

      <path className="cw-map-art__route" d="M357 88 349 196 364 303 351 408 376 513 397 615 371 711 333 811 345 919" />

      <g className="cw-map-art__sector-labels">
        <text x="342" y="118">NORTH SECTOR</text>
        <text x="341" y="490">CENTRAL SECTOR</text>
        <text x="297" y="778">SOUTH SECTOR</text>
      </g>

      <g className="cw-map-art__corner-marks">
        <path d="M24 64V24H64M656 24H696V64M24 936V976H64M656 976H696V936" />
      </g>
    </svg>
  );
}

function ownerDetails(card, players) {
  const owner = players.find((player) => player.playerId === card.ownerId);
  if (!owner) return { code: null, name: null };
  return { code: `P${owner.seatIndex + 1}`, name: owner.name };
}

function StrongholdToken({ card, position, players, active, legal, clanIndex, onInspect }) {
  const owner = ownerDetails(card, players);
  const ownership = card.locked
    ? 'Clan secured'
    : owner.name
      ? `Held by ${owner.name}`
      : card.central
        ? 'Open central stronghold'
        : 'Unavailable';

  return (
    <div
      className="cw-map-token-wrap"
      style={{ left: `${position.x}%`, top: `${position.y}%` }}
      data-map-position={`${position.x},${position.y}`}
    >
      <button
        type="button"
        className={`cw-map-token cw-map-token--clan-${clanIndex % 6} ${active ? 'cw-map-token--active' : ''} ${legal ? 'cw-map-token--legal' : ''} ${card.ownerId ? 'cw-map-token--held' : ''} ${card.locked ? 'cw-map-token--locked' : ''} ${card.kingsLanding ? 'cw-map-token--throne' : ''}`}
        aria-label={`Open ${card.name} details. ${card.id}, ${card.points} VP. ${ownership}${card.kingsLanding ? '. Iron Throne stronghold' : ''}.`}
        aria-pressed={active}
        data-stronghold-id={card.id}
        onClick={() => onInspect(card.id)}
      >
        <span className="cw-map-token__vp">{card.points} VP</span>
        <strong className={`cw-map-token__place ${card.name.length > 9 ? 'cw-map-token__place--long' : ''}`}>{card.name}</strong>
        {owner.code && <span className="cw-map-token__owner" aria-hidden="true">{owner.code}</span>}
        {card.locked && <span className="cw-map-token__lock" aria-hidden="true" />}
        {card.kingsLanding && <span className="cw-map-token__throne" aria-hidden="true">K</span>}
      </button>
    </div>
  );
}

export default function WesterosCampaignMap({
  campaign,
  strongholds,
  players,
  activeTargetId,
  legalTargetIds = [],
  onInspect,
}) {
  const layout = MAP_LAYOUTS[campaign] || {};
  const clans = [...new Set(strongholds.map((card) => card.clan))];
  const mapped = strongholds.filter((card) => layout[card.id]);
  const unmapped = strongholds.filter((card) => !layout[card.id]);
  const legalTargets = new Set(legalTargetIds);

  return (
    <div className="cw-map-board">
      <p id="cw-map-instructions" className="cw-map-board__instructions">
        Select a token to inspect its capture requirements. On compact screens, drag or scroll the map to explore.
      </p>
      <div
        className="cw-map-viewport"
        tabIndex="0"
        role="region"
        aria-label="Interactive Westeros campaign map"
        aria-describedby="cw-map-instructions"
      >
        <div className="cw-map-canvas" data-map-campaign={campaign || 'UNKNOWN'}>
          <WesterosMapArt />
          {mapped.map((card) => (
            <StrongholdToken
              key={card.id}
              card={card}
              position={layout[card.id]}
              players={players}
              active={activeTargetId === card.id}
              legal={legalTargets.has(card.id)}
              clanIndex={clans.indexOf(card.clan)}
              onInspect={onInspect}
            />
          ))}
        </div>
      </div>

      {unmapped.length > 0 && (
        <div className="cw-map-unmapped" role="group" aria-label="Unmapped strongholds">
          <p className="arcade-eyebrow">Unmapped strongholds</p>
          <div className="cw-map-unmapped__list">
            {unmapped.map((card) => (
              <button key={card.id} type="button" onClick={() => onInspect(card.id)}>
                <span>{card.id}</span>
                <strong>{card.name}</strong>
                <small>{card.points} VP</small>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
