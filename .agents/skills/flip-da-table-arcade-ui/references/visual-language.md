# Visual language

## Character

The product should feel like a well-maintained multiplayer cabinet: energetic, direct, tactile, and legible. Nostalgia is expressed through geometry and feedback rather than visual noise.

## Source of truth

Read `frontend/src/styles/tokens.css` before adding or changing any color, font, spacing, border, shadow, layer, or motion value. Use semantic variables rather than palette names in components.

## Typography

- Display: Press Start 2P. Use for the wordmark, page titles, scores, short all-caps labels, and game-state callouts.
- Body: Inter. Use for paragraphs, form controls, player names, instructions, and event logs.
- Mono: the system monospace stack. Use only for session IDs, game IDs, and compact numeric telemetry.
- Never set long paragraphs, logs, or form values in the display face.

## Geometry and depth

- Base spacing unit: 4px. Prefer 8, 12, 16, 24, 32, and 48px groupings.
- Default border: 2px solid. Separate major regions with a double-line or a small label notch, not a new shadow style.
- Default elevation: 4px hard offset. Pressed controls translate by 2px and reduce the shadow.
- Corners: square by default; 2-4px only where clipping or card handling benefits from it.
- Background effects may use a static pixel grid and very faint scanlines. Effects must sit behind content and never reduce text contrast.

## Theme variants

- `neutral`: cyan primary, magenta secondary, yellow focus and callout.
- `uno`: keep the shared cabinet; use the four UNO gameplay colors only for cards, active color, and immediate game feedback.
- `dvc`: use ink, warm white, and cyan. Magenta is reserved for destructive/error emphasis so the tiles remain the visual focus.

## Iconography and artwork

- Icons use an 8px or 16px grid, square caps, and `shape-rendering="crispEdges"` for SVG.
- Pair unfamiliar icons with text. Icon-only buttons require an accessible name.
- Game covers are original abstractions: a four-color card fan for UNO and a black/white code tile rack for DVC. Do not recreate official logos or branded card backs.
