# Sources and licenses

This skill distills visual principles; it does not vendor either framework.

## Retro Design System

- Source: https://github.com/NovusGFX/retro-design-system
- Reference: `styles/11-8bit-arcade` and its tokens.
- License: MIT.
- Adapted ideas: dark arcade palette, hard pixel borders and shadows, cabinet panels, compact HUD patterns.

## NES.css

- Source: https://github.com/nostalgic-css/NES.css
- License: MIT for code; documentation is separately licensed by its project.
- Adapted ideas: mature 8-bit control states, pressed-button depth, component contrast, and Press Start 2P as an English display font.
- NES.css is not a runtime dependency and its class names are not copied.

## Hue

- Source: https://github.com/dominikmartn/hue
- License: MIT.
- Adapted ideas: explicit skill metadata, a single source of truth for tokens, placeholder-free reference files, validation before handoff, and automated contrast/accessibility gates.
- This repository keeps executable CSS tokens as the canonical source instead of introducing a second YAML token model.

## Fonts

- Press Start 2P and Inter are distributed under the SIL Open Font License through their Fontsource packages.
- Keep package license files in installed dependencies and do not convert fonts into untracked external downloads.

## Artwork

All production cover art and pixel icons must be original geometric SVGs. Do not copy official UNO, Nintendo, arcade character, or Da Vinci Code artwork.
