# Motion, accessibility, and QA

## Motion

- Interaction feedback: 80-120ms.
- Panel/dialog entrances: no more than 180ms.
- Use transform and opacity where possible. Avoid layout-thrashing animation.
- No infinite flashing, bouncing, or pulsing. A static scanline texture is sufficient.
- Under `prefers-reduced-motion: reduce`, remove decorative transitions and transforms.

## Accessibility

- Body text contrast: at least 4.5:1. Large text and control boundaries: at least 3:1.
- Minimum pointer target: 44x44px, except non-interactive game cards where the selected hit area still meets the target.
- All actions must be keyboard reachable. Focus must never be hidden behind overlays.
- Dialogs require an accessible name and predictable close behavior.
- Status changes that affect the turn or connection use an appropriate live region without announcing decorative updates.
- Rotation guidance must be dismissible and must not permanently block navigation.

## Review matrix

1. Render default, loading, empty, error, disabled, full-capacity, reconnecting, and completed states.
2. Test 390x844 for ordinary screens and 667x375 for both games.
3. Test UNO with ten players and DVC with four players plus long names.
4. Verify keyboard order, visible focus, Escape behavior, reduced motion, and 200% zoom.
5. Run `npm run ui:audit`, `npm run lint`, `npm run test`, `npm run test:e2e`, and `npm run build`.
