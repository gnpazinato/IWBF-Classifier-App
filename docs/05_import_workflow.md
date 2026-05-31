# Import Workflow

## Supported Inputs

MVP targets:

- `.zip` containing `.docx` and/or `.pdf` files
- `.docx` entry lists
- `.docx` MIC lists
- `.pdf` lists, best effort

DOCX import should be the priority because IWBF-style documents often preserve tables that can be parsed reliably.

## Important Imported Fields

Required/high-priority:

- Team
- Player name
- Uniform number
- Imported Sport Class
- Sport Class Status (SCS)

Useful if present:

- IWBF ID
- Date of birth
- Health condition
- Impairment
- MIC notes
- Panel

## Import Review Philosophy

Imported data is a draft.

The user must be able to:

- accept all immediately;
- review and edit before import;
- correct later during the competition;
- delete teams that did not attend;
- remove players who are not competing;
- add replacement players;
- change uniform numbers.

## Import Screens

### 1. File Selection

User selects ZIP, DOCX, or PDF through Android system file picker.

### 2. Parse Summary

Show:

- files found;
- teams detected;
- players detected;
- files that could not be parsed;
- confidence warnings.

### 3. Review Teams

Each Team row:

- Team name/code
- source file
- number of players
- active toggle

Actions:

- edit team name/code;
- remove team from import;
- merge duplicate teams.

### 4. Review Players

Editable table:

- Team
- Number
- Player name
- Imported Sport Class
- SCS
- IWBF ID
- DOB
- MIC available flag

Actions:

- edit any cell;
- add player;
- remove player;
- move player to another team;
- accept all.

### 5. Import Report

Store the import report locally:

- original file names;
- parsed teams;
- parsed players;
- warnings;
- timestamp.

## Duplicate Handling

If the same player appears in Last Entry List and MIC List:

- Match by IWBF ID if available.
- Else match by normalized name and team.
- Merge MIC fields into the existing player.
- Preserve both source file names.

If there is a conflict:

- Keep entry-list values for number and imported class by default.
- Add a warning for user review.

## PDF Import

PDF import is best effort.

If table extraction fails:

- show raw text preview;
- let user manually create Teams/Players;
- allow copy/paste or typed entry.

## Sample Entry-List Shape

Typical entry-list table columns:

- Uniform number
- PLAYER (FAMILY NAME, given name)
- CLASS
- DD/MM/YY
- IWBF ID #
- SCS

Typical MIC-list table columns:

- #
- PLAYER (FAMILY NAME, given name)
- DD/MM/YY
- Health Condition
- Impairment
- CLASS
- SCS
- Notes
- PANEL

