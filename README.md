# Flip Da Table

English | [简体中文](README_ZH.md)

An online UNO (extensible to more turn‑based games) demo built with Spring Boot + React. The backend offers authentication, session management and a UNO runtime (rules engine + event log + state projection). The frontend provides auth pages, lobby, real‑time game play & spectating. Deployable via Docker Compose (PostgreSQL + backend + nginx‑served frontend).

## Tech Stack
- Backend: Spring Boot 3 (Web, Security, Data JPA, Validation, Actuator, SSE), Flyway, JWT (jjwt), Lombok
- Database: PostgreSQL + Flyway migrations (`backend/src/main/resources/db/migration`)
- Frontend: React 19, React Router v7, Vite, Tailwind CSS v4 + DaisyUI
- Realtime: Server‑Sent Events `/api/games/uno/{gameId}/stream`
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
    hooks/              # useUnoGame, usePlayAnimations
    components/         # Shared UI
      uno/              # UNO presentation & interaction components
    pages/              # Login, Lobby, PlayScreen, etc.
    assets/
nginx/
  nginx.conf

docker-compose.yml
```

## Backend Overview
- Auth: `/api/auth/register`, `/api/auth/login` (JWT; frontend uses `credentials: 'include'` for cookies if set)
- Sessions / Games: `/api/sessions` endpoints (create / query / join)
- UNO runtime:
  - `GET /api/games/uno/{gameId}/view?viewerId=...` perspective view (full hand only for the requesting player)
  - `POST /api/games/uno/{gameId}/commands` body `{ type, playerId, color?, value? }`
  - `GET /api/games/uno/{gameId}/stream` SSE broadcast (public, no private hands)
- Flyway migrations create user/role/session/game tables & state columns
- Configuration overridable via env: datasource + `APP_JWT_SECRET`

## Frontend Overview
- Pages in `src/pages`: Login, Register, Dashboard, Lobby, PlayScreen, SessionSummary
- `AuthContext` maintains user & token
- `src/api/*.js` isolates fetch logic
- `useUnoGame` hook manages initial fetch + SSE subscription + diff merge
- UNO component set under `components/uno`

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
```
# PowerShell example
$env:APP_JWT_SECRET = 'your-strong-secret'
docker compose up -d --pull always
```

---
This README provides a high-level overview; explore the source for details.
