# Competition Workflows

## Scenario A - Documents Are Provided Before the Competition

1. User creates a Competition.
2. User imports a ZIP, DOCX, or PDF package.
3. App parses entry-list documents and MIC lists.
4. App proposes Teams and Players.
5. User may review immediately or skip review.
6. App creates editable records.
7. During training or games, the user corrects numbers, names, classes, SCS, or teams as needed.

Required behavior:

- Import must never lock data.
- User can delete a Team from the Competition.
- User can remove a Player from a Team.
- User can add a Player during the event.
- User can change uniform number quickly.
- User can change imported Sport Class and SCS.

## Scenario B - No Documents Are Provided

1. User creates a blank Competition.
2. User creates Teams manually.
3. During a game, user enters player data as needed:
   - number;
   - name, if known;
   - class, if known;
   - SCS, if known.
4. User starts handwritten notes even if the player record is incomplete.

Required behavior:

- The app must allow placeholder players, such as `#8 Unknown`.
- The app must allow notes before name/class are known.
- Later corrections must preserve all handwritten notes and video markers.

## Classification Training Workflow

During Observation in Training, the user often records:

- Starting Sport Class for the competition;
- quick observations;
- early personal opinion;
- whether the player needs Observation in Competition.

The app should have a Training mode or training note section, but it can share the same player record.

Suggested player sections:

- Training Notes
- Game Observation Notes
- Panel Discussion Notes

## Game Observation Workflow

1. User opens a Game.
2. User selects Team A and Team B.
3. Optional: user loads a YouTube link or hides video panel.
4. User selects a Player chip.
5. User writes with S Pen.
6. User taps class/opinion/status buttons as needed.
7. Optional: user taps a video marker button.
8. Everything autosaves.

## Panel Discussion Workflow

After several games:

1. User filters Players by status: Discuss or Observe.
2. User opens Player record.
3. User shows handwritten notes and video evidence.
4. User updates Final Sport Class.
5. User updates Sport Class Status if needed.
6. User marks Player as Finalized.

## Class Fields

Use these as separate fields:

- Imported Sport Class: from entry list or MIC list.
- Starting Sport Class: class used to start the competition.
- My Opinion: classifier's personal working decision.
- Final Sport Class: panel decision.

The UI should show the three active decision fields clearly:

- Starting
- My Opinion
- Final

Imported Sport Class is reference metadata and can be shown near the player identity.

