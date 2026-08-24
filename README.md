# Flip Da Table

English | [简体中文](README_ZH.md)

An online UNO and Da Vinci Code table built with Spring Boot + React. The backend provides authentication, sessions, game runtimes, and real-time transport. The frontend uses a repository-owned neon 8-bit arcade design system across authentication, lobby, both games, and the final scoreboard. Deployable via Docker Compose (PostgreSQL + backend + nginx-served frontend).

## Tech Stack
- Backend: Spring Boot 3 (Web, Security, Data JPA, Validation, Actuator, SSE), Flyway, JWT (jjwt), Lombok
- Database: PostgreSQL + Flyway migrations (`backend/src/main/resources/db/migration`)
- Frontend: React 19, React Router v7, Vite, Tailwind CSS v4, repository-owned arcade components
- Realtime: UNO SSE, lobby/DVC WebSocket + STOMP
- Build: Gradle (Java 17 toolchain), Vite; Docker images `anguy491/flip-backend` & `anguy491/flip-frontend`

## Project Structure (condensed)
```
backend/
  build.gradle.kts
  src/main/java/com/flip/backend/
    api/                # REST + SSE controllers (Auth, Session, Game, Uno)
    game/               # Generic engine abstractions (phase, event, board, player)
      engine/event/     # GameEvent, EventQueue
      engine/phase/     # Phase, RuntimePhase
      entities/         # Board<P>, Player
    uno/                # UNO implementation (entities + events + phase + view)
      entities/         # UnoCard, UnoDeck, UnoPlayer, UnoBoard
      engine/
        event/          # UnoPlayCardEvent, UnoDrawCardEvent, ...
        phase/          # UnoRuntimePhase (core state machine)
        view/           # UnoView / UnoBoardView / UnoPlayerView
  src/main/resources/
    application.yml
    db/migration/
frontend/
  package.json
  src/
    api/                # fetch wrappers (auth, sessions, uno)
    context/            # AuthContext
    hooks/              # UNO and DVC network/state containers
    components/         # Shared arcade UI + pure game views
      arcade/           # Design-system primitives
      uno/              # UNO presentation & interaction components
      dvc/              # Da Vinci Code presentation components
    pages/              # Login, Lobby, PlayScreen, etc.
    assets/
  nginx.conf            # canonical reverse-proxy, security-header, and rate-limit config

docker-compose.yml
```

## Backend Overview
- Auth: email/password registration and login, non-enumerating password recovery, and optional Google Identity Services sign-in. JWTs include an account auth version so password changes revoke older tokens.
- Public auth endpoints include `/api/auth/capabilities`, `/api/auth/password/*`, and `/api/auth/google/*`; feature flags keep both additions independently reversible.
- Sessions / Games: `/api/sessions` endpoints (create / query / join)
- UNO runtime:
  - `GET /api/games/uno/{gameId}/view?viewerId=...` perspective view (full hand only for the requesting player)
  - `POST /api/games/uno/{gameId}/commands` body `{ type, playerId, color?, value? }`
  - `GET /api/games/uno/{gameId}/stream` SSE broadcast (public, no private hands)
- Flyway migrations create user/role/session/game tables & state columns
- Configuration is overridable via environment variables; see [`docs/AUTH_DEPLOYMENT.md`](docs/AUTH_DEPLOYMENT.md) for the Resend, Google, domain, and staged-release checklist.

## Frontend Overview
- Pages in `src/pages`: Login, Register, Forgot/Reset Password, Google callback/linking, Privacy, Dashboard, Lobby, UNO, DVC, and SessionSummary
- `AuthContext` maintains the authentication token
- `src/api/*.js` isolates fetch logic; REST/SSE/WebSocket contracts remain backend-compatible
- `useUnoGame` and `useDVCGame` own network/state concerns; `UnoGameView` and `DvcGameView` are deterministic presentation layers
- `src/styles/tokens.css` is the only numeric source for theme values; `src/styles/arcade.css` contains shared components and responsive behavior
- `/__ui-lab` renders fixed component and game states in development and is excluded from production builds

## Arcade Design System

The interface follows a dark arcade-cabinet shell with neutral, UNO, and DVC contexts. Press Start 2P is limited to short display text; Inter remains the body face. Components use 2px pixel borders, 4px hard shadows, visible yellow focus, short motion, and reduced-motion fallbacks.

Repository-level instructions live in `.agents/skills/flip-da-table-arcade-ui/`. The skill records component recipes, screen blueprints, accessibility checks, source licenses, original visual boards, and the UI audit implementation. It references Retro Design System and NES.css for visual calibration without importing either library.

## Frontend Development and QA

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173/__ui-lab` while the Vite development server is running. Useful checks:

```bash
npm run lint        # ESLint, React Hooks, JSX accessibility
npm run ui:audit    # no DaisyUI, arbitrary theme colors, soft shadows, or invalid motion
npm run test        # Vitest + React Testing Library
npm run test:e2e    # Playwright on Chromium, Firefox, and WebKit + axe
npm run build       # production bundle; UI Lab is excluded
npm run check       # lint + UI audit + unit tests + build
```

Chromium visual baselines for all UI Lab screens at desktop and mobile/landscape sizes are stored in `frontend/tests/ui-lab.spec.js-snapshots/`; `frontend/tests/ui-before/` retains the original login screen and bundle data for before/after review. GitHub Actions runs the same quality gates for frontend and design-skill changes. Self-hosted font notices are distributed from `frontend/public/licenses/`.

## Realtime Flow
1. Initial load: fetch `/view` for personalized snapshot
2. Open SSE stream for public updates `/stream`
3. Player submits commands (play, draw, choose color)
4. Backend applies events, broadcasts, client reconciles

## Design Highlights (UNO Engine)
Goal: clean layering between generic turn engine and concrete UNO rules, enabling new games with minimal friction.

1. Phase Abstraction
   - `Phase.enter()` for setup; `RuntimePhase.run()` for loops; `UnoRuntimePhase` implements turn advancement, command validation, stacking penalties, winner detection.
2. Circular Board Model
   - `Board<P>` uses a bidirectional ring for seating; supports `reverse()`, multi-step `step(k)`, turn counting. `UnoBoard` only adds top card & active color.
3. Event Driven Core
   - `GameEvent` + `EventQueue` decouple legality check (`isValid`) from side effects (`execute`). Easy to extend with new card effects or logging / replay.
4. Rule vs. State Separation
   - Runtime handles stacking accumulation & turn listener; events just signal penalties (`penaltyAmount`). Single responsibility per class.
5. View Projection Layer
   - `UnoView` families generate perspective-safe snapshots (hide other players' hands) before JSON shaping in controllers.
6. Turn Listener Injection
   - `setTurnListener` lets transport (SSE) attach after each turn without polluting engine concerns.
7. Bounded Action Log
   - Fixed-size log with sequence numbers enables incremental front-end reconciliation (`lastEventSeq`). Swappable for persistent storage.
8. Extensibility
   - New card: extend `UnoPlayCardEvent` logic or introduce additional game events.
   - New game: reuse Board/Phase/Event scaffolding; implement your `XxxRuntimePhase` & events.
9. Textual UML Sketch
   - Phase <- RuntimePhase <- UnoRuntimePhase
   - Board<P> <- UnoBoard; Player <- (UnoPlayer/UnoBot)
   - GameEvent <- (UnoPlayCardEvent, UnoDrawCardEvent, ...)
   - UnoRuntimePhase aggregates UnoBoard + UnoDeck + EventQueue

Design aligns with SRP, OCP, and clear separation of concerns for testability.

## Docker Deployment

Authentication should first be deployed with both feature flags disabled. Complete the provider setup and production preflight in [`docs/AUTH_DEPLOYMENT.md`](docs/AUTH_DEPLOYMENT.md), then enable password recovery and Google sign-in separately.

For local Google sign-in, Mailpit password-reset email, and PostgreSQL setup, follow [`docs/AUTH_LOCAL_DEVELOPMENT.md`](docs/AUTH_LOCAL_DEVELOPMENT.md) with `docker-compose.dev.yml`.

```
# PowerShell example
$env:APP_JWT_SECRET = 'your-strong-secret'
$env:POSTGRES_PASSWORD = 'your-strong-database-password'
docker compose up -d --pull always
```

---
This README provides a high-level overview; explore the source for details.
