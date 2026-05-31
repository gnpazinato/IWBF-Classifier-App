# MVP Build Plan

## Phase 0 - Repository Setup

- Create repository: `iwbf-classifier-app`.
- Add this planning pack to the repository.
- Add Android project skeleton.
- Configure GitHub Actions for Android build.
- Configure optional GitHub Pages web prototype only if needed.

## Phase 1 - Local Data Foundation

Deliverables:

- Competition create/list/open.
- Local JSON repository.
- Team CRUD.
- Player CRUD.
- Soft delete and restore.
- Export/import internal competition JSON package.

Acceptance:

- User can create a Competition.
- User can create Teams and Players manually.
- User can edit Team, number, name, imported class, SCS.
- User can remove a Player without losing historical data.

## Phase 2 - Import

Deliverables:

- ZIP picker.
- DOCX file discovery.
- Entry-list parser.
- MIC-list parser.
- Import review screen.
- Accept all without detailed review.
- Import report.

Acceptance:

- Sample Madrid/Guadalajara ZIP imports Teams and Players.
- User can correct all imported fields.
- User can skip review and correct later.

## Phase 3 - Pen-First Observation Screen

Deliverables:

- Landscape observation layout.
- Optional video panel placeholder.
- Team/player chip rails.
- Current Player header.
- Handwriting canvas.
- Pen, eraser, undo, redo.
- Autosave.

Acceptance:

- User can switch between 24 players quickly.
- Notes remain attached to correct Player.
- S Pen writes comfortably.
- Finger can navigate without unwanted writing.

## Phase 4 - Classification Fields

Deliverables:

- Imported Sport Class display.
- Sport Class Status display/edit.
- Starting Sport Class field.
- My Opinion field.
- Final Sport Class field.
- Observation Status field.
- Quick class selector.

Acceptance:

- User can record three different class decisions.
- Imported class remains visible but editable.
- Player can be marked Discuss or Finalized.

## Phase 5 - Video Markers

Deliverables:

- Optional YouTube link per Game.
- Hide/show video panel.
- Flag Moment.
- Pending Video Link list.
- Link Current Video.
- Replay marker with start/end/speed.

Acceptance:

- App works with no video.
- User can flag a live moment and link it when video appears.
- Markers attach to Player records.

## Phase 6 - Local MP4 Attachments

Deliverables:

- Attach MP4 to Player.
- Copy MP4 into competition storage.
- Playback local MP4.
- Delete attachment.
- Export attachments in ZIP.

Acceptance:

- User can attach a screen recording or authorized clip.
- Clip remains linked to Player after export/import.

## Phase 7 - Export and Review

Deliverables:

- Export Competition ZIP.
- Optional PDF summary by Player/Team.
- Discussion screen with filters.
- Storage usage screen.

Acceptance:

- User can back up a competition.
- User can show Player notes and evidence during panel discussion.
- User can delete old clips after final decision.

## MVP Cut Line

The first usable field MVP can omit:

- OCR;
- handwriting recognition;
- PDF generation;
- local MP4 trimming;
- automatic livestream delay calibration;
- Google Drive API integration;
- cloud sync.

Do not omit:

- editable imported roster;
- S Pen handwriting;
- fast player switching;
- class fields;
- local export.

