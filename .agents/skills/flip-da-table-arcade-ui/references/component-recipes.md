# Component recipes

All components use semantic tokens from `frontend/src/styles/tokens.css` and expose native HTML props unless noted.

## Shell and panels

- `ArcadeShell`: full viewport background, skip link, static grid/scanline decoration, and `data-game="neutral|uno|dvc"`.
- `ArcadePanel`: 2px outline plus 4px hard shadow. Optional eyebrow/title slot. Avoid nested panels unless hierarchy requires it.
- `ArcadeToolbar`: horizontally groups context, connection state, and navigation; wraps on ordinary pages and remains compact on game screens.

## Controls

- `ArcadeButton`: primary, secondary, ghost, danger variants; sm/md sizes; loading must retain label meaning and prevent duplicate activation.
- `ArcadeInput` and `ArcadeSelect`: visible label, optional hint/error, 44px minimum target, high-contrast placeholder, and consistent disabled state.
- `ArcadeDialog`: labelled modal, initial focus, Escape close when dismissal is allowed, focus containment, backdrop, and body scroll lock.
- `ArcadeBadge` and `StatusBanner`: pair color with text/icon; tones are neutral, info, success, warning, and error.

## Game primitives

- `PlayerSeat`: name, type, ready/current status, hand count when relevant, and a square pixel avatar marker. Long names truncate visually but remain available through `title`.
- `Scoreboard`: semantic table on larger screens and readable stacked rows on narrow screens.
- Cards and tiles keep their own game semantics; shared panels must not recolor them indiscriminately.

## Required states

Every interactive component must define default, hover, active, keyboard focus, disabled, and busy behavior. Data regions must define loading, empty, error, stale/reconnecting, and success feedback where relevant.
