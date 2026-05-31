# Claude Code Project Instructions

You are building `IWBF Classifier App`, a private Android tablet app for wheelchair basketball classification observation.

## Primary User

The primary user is an IWBF classifier using a Samsung Galaxy Tab with S Pen during real competitions. The user observes players live and needs to move quickly between athletes while writing handwritten notes.

## Product Priorities

1. Pen-first observation speed.
2. Local-first data ownership.
3. Easy correction of imported or manually entered data.
4. Clear IWBF terminology.
5. Optional video evidence without making video mandatory.
6. Exportable competition records.

## UX Rules

- The app UI must be in English.
- Use IWBF-style terminology: Player, Team, Competition, Game, Sport Class, Sport Class Status, New, Review, Confirmed, Observation Assessment, First Appearance, Classification Panel.
- Follow `docs/12_visual_design_direction.md` before implementing UI. The app should feel modern, premium, minimal, and field-ready, with an IWBF-inspired dark/charcoal/gold visual language and a light paper-like handwriting canvas.
- S Pen writes. Finger navigates.
- Avoid typing during game flow unless editing roster metadata.
- Player switching must be one tap.
- Autosave everything.
- Avoid popups while observing a game.
- Make all imported data editable.
- Make deleting/removing a player from the competition easy but reversible where possible.

## MVP Scope

Build native Android first unless explicitly told to build only a web prototype.

Core MVP:

- Create Competition.
- Import ZIP/DOCX/PDF roster documents.
- Review imported Teams and Players.
- Create blank Competition and Teams manually.
- Edit Team, Player number, Player name, imported Sport Class, Sport Class Status, MIC notes.
- Track Starting Sport Class, My Opinion, Final Sport Class.
- Main Game Observation screen with optional video panel.
- Handwritten note canvas per Player.
- Player chips/roster list for fast switching.
- Video evidence markers for YouTube links.
- Attach local MP4 clips to Player records.
- Export Competition as ZIP.

## Technical Preferences

- Kotlin.
- Jetpack Compose.
- AndroidX Ink API or equivalent high-quality stylus implementation.
- Local JSON files for structured data.
- Local binary files for ink strokes, PNG previews, and MP4 attachments.
- Storage Access Framework for import/export.
- Keep all data in a human-inspectable folder structure.

## Important Constraints

- Do not design this as a cloud product.
- Do not require user login.
- Do not rely on a backend.
- Do not download YouTube video content.
- YouTube video evidence should be stored as timestamps/markers unless the user imports or attaches a local MP4.
- The app must work even when there is no livestream.

## Read Before Coding

Read all files in `docs/` before implementing.
