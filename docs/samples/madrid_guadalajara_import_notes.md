# Sample Import Notes - Madrid / Guadalajara ZIP

Sample file inspected:

`Madrid _ Guadalajara.zip`

## Files Found

- `ARG F - Final Entry List.docx`
- `AUS F - Last Entry List.docx`
- `COL F - Last Entry List.docx`
- `ESP F - Last Entry List.docx`
- `ESP MIC List .docx`
- `FRA F - Last Entry List.docx`
- `FRA MIC List .docx`
- `GER F - Last Entry List.docx`
- `THA F - Last Entry List.docx`

## Entry List Pattern

Most entry-list files contain:

- event/location table;
- NOWB/team table;
- player table;
- SCS explanation table.

Player table columns are typically:

- Uniform number
- PLAYER (FAMILY NAME, given name)
- CLASS
- DD/MM/YY
- IWBF ID #
- SCS

The player table may have around 23 rows, including header rows and blank rows. Importer should ignore blank rows and non-player rows.

## MIC List Pattern

MIC list files contain:

- event/location/team table;
- MIC player table.

MIC player columns are typically:

- #
- PLAYER (FAMILY NAME, given name)
- DD/MM/YY
- Health Condition
- Impairment
- CLASS
- SCS
- Notes
- PANEL

## Teams Observed

Detected team-like file prefixes:

- ARG F
- AUS F
- COL F
- ESP F
- FRA F
- GER F
- THA F

## Importer Requirements From Sample

- Handle `Uniform number` split across two cells or lines.
- Handle class values with dot or comma, such as `4.0` and `4,0`.
- Handle missing dates or IDs.
- Handle files named `Last Entry List` or `Final Entry List`.
- Merge MIC list records into entry-list players for the same team.
- Preserve source file names for audit/debug.

## Example Parsed Player Fields

Entry-list player examples:

- number: `4`
- name: `VINCI, Sarah`
- importedSportClass: `1.0`
- dateOfBirth: `04/12/91`
- iwbfId: `PF111051`
- sportClassStatus: `C`

MIC-list player examples:

- number: `20`
- name: `WEMBOLUA Grace`
- healthCondition: `Double amputee BK`
- impairment: `LD / L`
- importedSportClass: `4.0`
- sportClassStatus: `N`
- panel: `EB / TS`

