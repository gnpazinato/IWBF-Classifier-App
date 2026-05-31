# Pen-First UX

## Device Assumption

Primary device: Samsung Galaxy Tab with S Pen.

The app must be optimized for landscape orientation during games.

## Input Model

- S Pen writes or erases.
- Finger scrolls, pans, changes player, taps controls.
- Keyboard entry is allowed for roster edits.
- Do not require typing for observation notes.

## Main Observation Layout

Landscape mode:

- Top area: optional video panel.
- Bottom area: handwritten note canvas.
- Left or right rail: Team A and Team B player chips.
- Compact header: Competition, Game, current Player, class fields.
- Floating tools: pen, eraser, undo, redo, marker, video evidence.

If no video is available:

- Hide video panel.
- Expand note canvas.
- Keep player chips visible.

## Player Switching

Player switching must be one tap.

Each chip should show:

- uniform number;
- short name or initials;
- current observation status color;
- small class indicator.

Example:

- `#7 ALVAREZ 4.5`
- `#10 UNKNOWN`
- `#4 N 1.0`

## Writing Canvas

Each Player should have a large freehand canvas.

Recommended note pages:

- Main Notes
- Key Moments
- Forward Plane
- Vertical Plane
- Sideway Plane
- Rationale
- Panel Notes

For MVP, it is acceptable to provide one infinite or paged canvas plus optional quick labels.

## Quick Class Controls

Provide large tappable class buttons:

- 1.0
- 1.5
- 2.0
- 2.5
- 3.0
- 3.5
- 4.0
- 4.5
- NE

The class control should update the selected field:

- Starting Sport Class
- My Opinion
- Final Sport Class

Use a segmented control to choose which field is being edited.

## Editing Roster Data

Roster metadata can use typed input.

Editing must be available from:

- import review screen;
- team roster screen;
- player detail screen;
- quick edit sheet during game.

Quick edit sheet fields:

- Team
- Number
- Player name
- Imported Sport Class
- Sport Class Status
- Starting Sport Class

The quick edit sheet must not cover the whole writing canvas unless necessary.

## Autosave

Autosave on:

- stroke end;
- player switch;
- class change;
- video marker creation;
- app backgrounding;
- navigation away from screen.

Show a subtle saved indicator. Do not interrupt the user.

