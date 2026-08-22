# Screen blueprints

## Auth

Use a single cabinet marquee and a focused credential panel. Keep the route switch as a real link. On desktop, allow a decorative side panel; collapse to one column below 768px.

## Dashboard

Lead with player identity and two selectable original game cartridges. Creation acts on the selected cartridge. Join and profile use shared dialogs. Show only UNO and Da Vinci Code.

## Lobby

Make the session code and invite action prominent. Represent members as player slots instead of a generic zebra table. Host-only actions must be visibly explained. Keep rounds, readiness, capacity, and connection state in one control console.

## UNO

Order: compact toolbar, horizontally scrollable opponent seats, central table, state instruction, player hand/actions. The discard and active color are the strongest central signals. The event log may collapse on phone landscape. Support ten seats without shrinking names below readability.

## Da Vinci Code

Order: compact toolbar, opponent racks, own rack, pending tile, phase instruction, one primary action group. Black and white tiles dominate; controls stay visually quiet until actionable. Preserve drag, select, reveal, and insertion affordances.

## Summary

Present the winner as a high-score callout, then a semantic results table and compact pixel podium. Support no-data, ties, and more than three players.

## Responsive targets

- Ordinary pages: 390x844, 768x1024, and 1440x900.
- Game pages: 667x375 landscape, 1024x768, and 1440x900.
- Page-level horizontal overflow is a defect. Only explicitly scrollable seat, hand, rack, and table regions may overflow.
