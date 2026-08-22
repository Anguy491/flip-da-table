---
name: flip-da-table-arcade-ui
description: Build, redesign, or review Flip Da Table frontend pages and components using the repository's neon 8-bit arcade design system. Use for UI layout, styling, responsive behavior, motion, visual assets, accessibility, or frontend visual QA; do not use for backend-only, database, or game-rule work.
---

# Flip Da Table Arcade UI

Keep every frontend change recognizably part of the same dark neon arcade cabinet while preserving fast scanning during multiplayer play.

## Workflow

1. Inspect the existing screen, its data states, and nearby shared components before changing it.
2. Select the shell variant: `neutral` for auth/dashboard/lobby/summary, `uno` for UNO, or `dvc` for Da Vinci Code.
3. Compose from shared arcade primitives and semantic tokens. Do not add a one-off visual language inside a page.
4. Render representative empty, loading, error, active, disabled, full-capacity, and finished states at the required viewports.
5. Run `npm run ui:audit`, lint, tests, and the production build before handoff.

The canonical token values live in `frontend/src/styles/tokens.css`. Never duplicate their values in skill documentation or JSX.

## Non-negotiable rules

- Use pixel typography only for headings, short labels, scores, and marquee text. Keep body copy and logs in the readable body face.
- Use 2px borders, 4px hard offset shadows, a 4px spacing grid, and only 0-4px corner radii.
- Keep keyboard focus conspicuous. Do not communicate state by color alone.
- Keep motion short and purposeful. Disable decorative motion for `prefers-reduced-motion`.
- Do not introduce DaisyUI classes, NES.css, glassmorphism, soft SaaS cards, arbitrary colors, continuous flicker, emoji-only controls, or copied game/trademark artwork.
- Preserve REST, SSE, WebSocket, router, and game-rule behavior unless the user explicitly requests a behavioral change.

## Read the relevant reference

- For tokens, typography, surfaces, iconography, and game variants, read [references/visual-language.md](references/visual-language.md).
- For shared controls and interaction states, read [references/component-recipes.md](references/component-recipes.md).
- For page or game layout work, read [references/screen-blueprints.md](references/screen-blueprints.md).
- For motion, responsive, accessibility, or review work, read [references/motion-accessibility-qa.md](references/motion-accessibility-qa.md).
- Before importing or adapting external material, read [references/sources-and-licenses.md](references/sources-and-licenses.md).

Use `assets/visual-board.svg`, `assets/component-states.svg`, `assets/arcade-mark.svg`, and `assets/pixel-icons.svg` as visual references, not as mandatory production assets.
