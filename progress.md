Original prompt: PLEASE IMPLEMENT THIS PLAN: LASVEGAS standard bot support with one standard server-authoritative strategy, 800ms visible roll/place steps, 1 human + 2 bots minimum, persisted recovery, lobby/table UI, documentation, and comprehensive verification.

## Progress

- 2026-08-26: New continuation request: move the LASVEGAS roll reveal out of the action console into a large centered modal, add an eight-die staggered spin/settle sequence, roulette motion, and original spark/metal-friction particles using Motion plus a lightweight Canvas layer.
- 2026-08-26: Added Motion 13, a server-authoritative roll-reveal state, a non-dismissible centered arcade dialog, responsive eight-die roulette staging, left-to-right impacts, deterministic Canvas spark bursts, and a reduced-motion static reveal. Removed rolled dice from the action console and kept choices hidden until the reveal finishes.
- 2026-08-26: Added a deterministic eight-die UI Lab roll fixture, component coverage, cross-browser modal/focus/reduced-motion checks, and refreshed the three intentional Las Vegas visual baselines. Manual desktop and 667x375 screenshot review confirmed centered placement, visible sparks, and no clipping.
- 2026-08-26: Began the persistent-action continuation: replaced the roll artwork with six-faced CSS 3D cubes driven by the existing lazy Motion runtime, split bounce from cube rotation, and refactored the roll session so completion reveals in-dialog Place/Skip controls instead of closing the modal.
- 2026-08-26: Added completed-roll recovery through Action Console Show, completion-only Hide/Escape, server-confirmed action dismissal, retry-in-place errors, explicit focus restoration, and updated deterministic UI Lab state/text hooks for the full flow.
- 2026-08-26: Browser screenshot review caught and fixed an inherited infinite opacity repeat on non-reduced-motion dice. Final desktop, 667×375 landscape, and 390×844 portrait inspections show opaque multi-face cubes, all actions reachable, and no page overflow.
- 2026-08-26: Persistent 3D roll continuation complete. Final Arcade UI audit, ESLint, 44 Vitest tests, production build, and 78 Playwright cases across Chromium, Firefox, and WebKit passed; intentional Vegas desktop/tablet/landscape visual baselines were refreshed.
- 2026-08-26: Began the roll-pacing polish: widened inter-die spacing, strengthened the big die with a thicker two-tone hard border, and changed the reveal to spin for five seconds before settling one die per second from left to right.
- 2026-08-26: Added fake-timer coverage for the exact 5-second lead and 1-second settle cadence. The focused 14-test Las Vegas component suite and Arcade UI audit pass.
- 2026-08-26: Roll-pacing polish complete. Browser screenshots confirmed wider spacing, a visibly stronger big-die border, and progressive left-to-right locking; final ESLint, UI audit, 44 Vitest tests, production build, and 78 Playwright cases across Chromium, Firefox, and WebKit pass.
- 2026-08-26: Shortened the initial roll spin from five seconds to three seconds while preserving the one-die-per-second left-to-right settle cadence; updated the exact timer boundary test accordingly.
- 2026-08-26: The three-second spin adjustment is verified by browser screenshots, 44 Vitest tests, lint/UI audit/build, and the key persistent-roll Playwright case in Chromium, Firefox, and WebKit.
- 2026-08-25: Began implementation from a clean worktree. Selected server-side strategy, transactional step tickets, and existing arcade UI primitives per the approved plan.
- 2026-08-25: Added Bot identity propagation, schema-v2 snapshot output with v1 acceptance, a public-information-only deterministic strategy, and the first focused tests.
- 2026-08-25: Added guarded transactional Bot steps, delayed scheduling, recovery scanning, per-step persistence/broadcasting, and Bot metrics. Local Gradle verification is pending because no JDK is currently installed/discoverable.
- 2026-08-25: Enabled LASVEGAS lobby Bot seats, added CPU badges and live turn copy, updated fixtures/tests, and exposed a concise `render_game_to_text` browser-test projection.
- 2026-08-25: Temporary JDK 17 enabled real Gradle compilation. Added v1 migration, split Bot-step, stale-ticket, consecutive-Bot, human-to-Bot handoff, and recovery de-duplication tests.
- 2026-08-25: Updated the canonical Chinese Las Vegas rules for mixed human/Bot tables, server authority, fair information, strategy, and the 800ms presentation cadence.
- 2026-08-25: Added a deterministic UI Lab Bot-turn state and Playwright assertions for CPU copy, locked human controls, and `render_game_to_text` parity.
- 2026-08-25: First Bot-turn Playwright pass found an ambiguous CPU text locator; tightened it to the exact badge before rerunning.
- 2026-08-25: Browser-client screenshot review confirmed visible CPU turn copy and locked controls, and exposed a fixture mismatch (Bot had a roll but zero remaining dice); corrected the fixture to four remaining dice.
- 2026-08-25: Added a bounded full three-round mixed-table executor test that reaches final settlement through Bot steps and marks both game and session ended.
- 2026-08-25: Hardened startup scheduling so view generation inside the outer start transaction only schedules after commit, and normalized Bot names/readiness server-side.
- 2026-08-25: Prevented duplicate lobby Bot display names when a Bot is removed and another is added.
- 2026-08-25: Full frontend check passed functionally but reported one hooks dependency warning in UI Lab; memoized the derived Bot fixture to remove it.
- 2026-08-25: Added an explicit restart-boundary test proving a Bot's persisted roll restores in `WAITING_FOR_CHOICE` with the same guarded scheduling ticket.
- 2026-08-25: Final review enforced human-first/Bot-appended seating even for interleaved input, locked every strategy tie-break into a test, and added an authenticated-human-versus-Bot identity test.
- 2026-08-25: Set the server-side Bot presentation delay to zero for the Gradle test environment while retaining the production default of 800ms.
- 2026-08-25: Ran the PostgreSQL Testcontainers suite through the active Colima socket and upgraded its restart case to restore a persisted `BOT1` roll/choice boundary plus human identity mappings.
- 2026-08-25: Added a deterministic Playwright 1-human/2-Bot state sequence covering both Bots' separate roll/choice broadcasts and the handoff back to the human, plus Las Vegas portrait overflow coverage.
- 2026-08-25: The first sequence run exposed a test-start race before the UI Lab text hook mounted; added an explicit readiness wait. Lengthened only the test recovery scan interval to prevent scheduled scans outliving Testcontainers contexts.

## TODO

- None. Implementation and the final backend/frontend/browser verification matrix are complete.

## Final verification

- Backend: 174 tests passed, including PostgreSQL/Testcontainers restart and row-lock integration cases; 0 skipped, failed, or errored.
- Frontend: ESLint passed; 41 Vitest tests passed; Arcade UI audit passed; production build passed.
- Browser: 72 Playwright cases passed across Chromium, Firefox, and WebKit, including the 1-human/2-Bot sequence and 390×844, 667×375, 1024×768, and 1440×900 checks.
- Patch: `git diff --check` passed.
- 2026-08-26 continuation: Arcade UI audit, ESLint, 43 Vitest tests, production build, and 78 Playwright cases across Chromium, Firefox, and WebKit passed; `git diff --check` also passed.
- 2026-08-26 persistent 3D continuation: Arcade UI audit and ESLint passed; 44 Vitest tests and 78 Playwright cases passed; production build and `git diff --check` passed.
