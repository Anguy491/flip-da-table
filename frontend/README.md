# Flip Da Table Frontend

React 19 + Vite + Tailwind CSS 4 frontend for the Flip Da Table multiplayer arcade.

```bash
npm install
npm run dev
```

Development-only UI Lab: `http://localhost:5173/__ui-lab`

```bash
npm run lint
npm run ui:audit
npm run test
npm run test:e2e
npm run build
npm run check
```

Design tokens live in `src/styles/tokens.css`; shared arcade components live in `src/components/arcade/`. Repository design instructions and references are in `../.agents/skills/flip-da-table-arcade-ui/`. Visual baselines live in `tests/ui-lab.spec.js-snapshots/`, with the original login screenshot and bundle data retained in `tests/ui-before/`. See `tests/ACCEPTANCE.md` for the current verification report. Bundled font notices are shipped from `public/licenses/`.
