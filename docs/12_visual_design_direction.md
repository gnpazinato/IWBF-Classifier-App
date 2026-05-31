# Visual Design Direction

The app does not need to be decorative, but it should feel modern, premium, focused, and credible. It should look like a professional IWBF-adjacent classification tool, not like a generic notes app.

This direction is based on public IWBF web presence observed on `iwbf.org`, especially:

- the dark institutional site shell;
- the use of real wheelchair basketball imagery;
- the compact document/resource style on the Downloads page;
- the Classification navigation structure;
- the recurring dark charcoal, white, muted gray, and bronze/gold accent palette.

Reference links:

- https://www.iwbf.org/
- https://www.iwbf.org/downloads
- https://www.iwbf.org/about-iwbf

## Important Brand Boundary

This is a private classifier app. Do not imply it is an official IWBF product unless the user later confirms permission.

Do:

- use IWBF terminology;
- use an IWBF-inspired professional palette;
- use a restrained, basketball-classification visual language.

Do not:

- embed official IWBF logos by default;
- scrape official images into the app;
- label the product as officially endorsed;
- overuse brand colors in a way that looks like a counterfeit official app.

Suggested app name display:

- `Classifier App`
- subtitle: `Wheelchair Basketball Observation`

Repository/project name can remain `IWBF Classifier App`.

## Design Personality

Keywords:

- premium
- minimal
- technical
- calm
- fast
- field-ready
- institutional
- tablet-native

The app should feel closer to a professional scorer's table / classification panel tool than to a consumer lifestyle app.

## Color System

Use a dark-first interface for game observation. It reduces glare in venues and matches the current IWBF site tone.

Core colors:

```text
Ink Black        #0E0E0E
Panel Black      #181818
Card Charcoal    #222222
Divider Gray     #3D3D3D
Text Primary     #FFFFFF
Text Secondary   #B8B8B8
Text Muted       #7C7C7C
IWBF Gold        #A3975D
Gold Soft        #A3975D26
Gold Border      #A3975D80
Paper Surface    #FAFAFA
Ink Stroke       #111111
Alert Red        #EA384C
Info Blue        #1C276D
```

Use color sparingly:

- Gold is for primary actions, active player, selected class field, and key evidence.
- Red is only for destructive actions or urgent warnings.
- Blue is only for informational video/import states.
- Most surfaces should be black/charcoal/white/gray.

## Light and Dark Areas

The app should combine:

- dark navigation, roster rails, headers, and video controls;
- light paper-like note canvas for handwriting.

This is important: the handwriting area should feel like paper, not like writing on a dark dashboard.

Suggested observation screen:

```text
Dark top/status area
Optional video panel
Dark player rails
Light handwriting canvas
Compact dark toolbars
```

## Typography

Use a clean sans-serif. Prefer Android system typography unless there is a strong reason to bundle fonts.

Recommended:

- Android system font / Roboto
- Use uppercase micro-labels for section labels
- Use normal case for data values and actions

Hierarchy:

- Screen title: 20-24sp, semibold
- Player name/header: 18-22sp, semibold
- Roster chips: 13-15sp, medium
- Metadata labels: 11-12sp, uppercase, letter spacing around 0.08em
- Body/data text: 14-16sp

Avoid giant marketing typography. This is a working tool.

## Shape and Spacing

Use modern but restrained rounding:

- Buttons: 6dp radius
- Panels/cards: 8dp radius
- Note canvas/page: 6dp radius
- Chips: 6dp radius

Avoid pill-heavy UI. Use squared professional components with mild rounding.

Spacing:

- Dense but breathable.
- 8dp base grid.
- Observation screen should prioritize visible canvas and player switching over empty space.

## Components

### Top App Bar

Dark background.

Contents:

- Competition name
- Game name
- save/sync indicator
- compact overflow menu

Use gold only for the active state or primary action.

### Player Chips

Player chips are critical.

Each chip should show:

```text
#7
ALVAREZ
4.5 C
```

States:

- Active: gold border or gold left rail
- Discuss: red dot or red left rail
- Observe: gold dot
- Finalized: muted gray/check
- Not observed: neutral charcoal

Do not make chips huge. The user may need 24 players visible.

### Class Selector

Use a compact segmented control for target field:

- Starting
- My Opinion
- Final

Then class buttons:

- 1.0
- 1.5
- 2.0
- 2.5
- 3.0
- 3.5
- 4.0
- 4.5
- NE

Selected class:

- gold fill;
- dark text;
- strong contrast.

### Handwriting Canvas

The canvas should feel like a premium digital worksheet:

- off-white background;
- subtle grid or faint ruled dots optional;
- no heavy borders;
- page shadow extremely subtle;
- dark ink stroke by default;
- gold highlighter option.

Toolbar:

- pen
- eraser
- undo
- redo
- page/section switch

Toolbar must be icon-first.

### Video Panel

Video panel should be optional and collapsible.

Controls:

- Load Link
- Hide
- Flag Moment
- Link Current Video
- Replay
- -5s
- +5s
- 0.25x
- 0.5x
- 1.0x

`Flag Moment` is the primary video action and should use gold.

Pending video markers should be visible but not intrusive.

### Import Review

Use a clean data-table style:

- dark header;
- light table body or charcoal rows;
- inline editable cells;
- gold primary action;
- warnings in muted red/amber;
- confidence badges if useful.

The import review should feel like a professional roster verification screen.

## Layout Principles

### Landscape Tablet First

Primary design target:

- Samsung Galaxy Tab in landscape.

Do not design phone-first.

Observation layout:

```text
┌──────────────────────────────────────────────────────────┐
│ Competition / Game / Save status / menu                  │
├───────────────┬──────────────────────────┬───────────────┤
│ Team A roster │ Optional video panel      │ Team B roster │
│               ├──────────────────────────┤               │
│               │ Player header/classes     │               │
│               ├──────────────────────────┤               │
│               │ Handwriting canvas        │               │
└───────────────┴──────────────────────────┴───────────────┘
```

If the video panel is hidden:

- expand the handwriting canvas vertically;
- keep player rails visible.

### Touch Targets

Because this is used during live games:

- primary controls: at least 44dp;
- player chips: at least 44dp tall where possible;
- destructive controls require confirmation outside the game flow;
- no tiny text-only tap targets for game-critical actions.

## Motion

Use minimal motion:

- quick crossfade for screen transitions;
- subtle active player highlight;
- no decorative animations during observation;
- no distracting loading effects.

## App Icon Direction

Do not use the official IWBF logo.

Possible icon concept:

- dark background;
- gold court key/arc line;
- small white classification mark or clipboard line;
- no wheelchair athlete silhouette unless custom-designed and clearly not copied.

## Claude Code Design Prompt

Use this prompt when asking Claude Code to begin UI implementation:

```text
Before implementing UI, read docs/12_visual_design_direction.md.

The app should look modern, premium, minimal, and field-ready, inspired by IWBF's public visual language without using official logos or implying endorsement.

Use a dark institutional shell with charcoal surfaces, white/gray text, and a muted gold accent. The handwriting canvas should be light and paper-like. Prioritize landscape tablet ergonomics, S Pen use, fast player switching, and dense professional layouts over decorative marketing design.

Create a small Compose design system first:

- AppColors
- AppTypography
- AppSpacing
- AppShapes
- primary/secondary/destructive buttons
- roster player chip
- class selector
- observation top bar
- paper note canvas container

Then apply it to the initial MVP screens. Keep the UI clean and purposeful, not flashy.
```

