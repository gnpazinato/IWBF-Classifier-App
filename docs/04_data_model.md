# Local Data Model

The MVP should use local files, not a server database.

Use structured JSON for metadata and separate files for ink strokes, rendered note previews, and video attachments.

## Folder Structure

Suggested exportable folder structure:

```text
competition-id/
  competition.json
  teams/
    team-id.json
  players/
    player-id.json
  games/
    game-id.json
  notes/
    player-id/
      main.ink
      key_moments.ink
      rationale.ink
      preview.png
  video_markers/
    marker-id.json
  attachments/
    attachment-id.mp4
  imports/
    original-files/
    import-report.json
```

## Competition

```json
{
  "id": "competition-uuid",
  "name": "IWBF 2026 Women's Repechage",
  "location": "Madrid / Guadalajara",
  "startDate": "2026-04-08",
  "endDate": null,
  "createdAt": "2026-05-31T12:00:00Z",
  "updatedAt": "2026-05-31T12:00:00Z"
}
```

## Team

```json
{
  "id": "team-uuid",
  "competitionId": "competition-uuid",
  "name": "Australia",
  "code": "AUS F",
  "gender": "Female",
  "active": true,
  "source": {
    "type": "import",
    "fileName": "AUS F - Last Entry List.docx"
  }
}
```

## Player

```json
{
  "id": "player-uuid",
  "competitionId": "competition-uuid",
  "teamId": "team-uuid",
  "uniformNumber": "4",
  "name": "VINCI, Sarah",
  "iwbfId": "PF111051",
  "dateOfBirth": "1991-12-04",
  "importedSportClass": "1.0",
  "sportClassStatus": "C",
  "startingSportClass": null,
  "myOpinionSportClass": null,
  "finalSportClass": null,
  "observationStatus": "Not Observed",
  "mic": {
    "healthCondition": null,
    "impairment": null,
    "notes": null,
    "panel": null
  },
  "active": true,
  "source": {
    "type": "import",
    "fileName": "AUS F - Last Entry List.docx",
    "confidence": 0.95
  },
  "createdAt": "2026-05-31T12:00:00Z",
  "updatedAt": "2026-05-31T12:00:00Z"
}
```

## Game

```json
{
  "id": "game-uuid",
  "competitionId": "competition-uuid",
  "name": "Australia vs Spain",
  "teamAId": "team-uuid-a",
  "teamBId": "team-uuid-b",
  "date": "2026-04-10",
  "youtube": {
    "enabled": true,
    "url": "https://www.youtube.com/watch?v=VIDEO_ID",
    "videoId": "VIDEO_ID",
    "streamDelaySeconds": 38,
    "delayCalibrated": true
  }
}
```

## Observation Event

Observation events are optional structured markers that sit beside freehand notes.

```json
{
  "id": "event-uuid",
  "competitionId": "competition-uuid",
  "gameId": "game-uuid",
  "playerId": "player-uuid",
  "type": "Key Moment",
  "createdAtDeviceTime": "2026-05-31T12:10:00Z",
  "gameClock": {
    "period": "Q2",
    "clock": "06:42"
  },
  "videoMarkerId": "marker-uuid",
  "freehandNoteRef": "notes/player-id/main.ink"
}
```

## YouTube Video Marker

This is not a downloaded video. It is a pointer to a moment in the YouTube player.

```json
{
  "id": "marker-uuid",
  "competitionId": "competition-uuid",
  "gameId": "game-uuid",
  "playerId": "player-uuid",
  "source": "youtube",
  "videoId": "VIDEO_ID",
  "startSeconds": 1234.5,
  "endSeconds": 1246.5,
  "playbackRate": 0.5,
  "label": "Forward reach after contact",
  "linkedFromEventId": "event-uuid",
  "createdAt": "2026-05-31T12:12:00Z"
}
```

## Local Video Attachment

```json
{
  "id": "attachment-uuid",
  "competitionId": "competition-uuid",
  "gameId": "game-uuid",
  "playerId": "player-uuid",
  "source": "local_file",
  "fileName": "attachment-id.mp4",
  "durationSeconds": 12.4,
  "label": "Screen recording - chair contact",
  "createdAt": "2026-05-31T12:14:00Z"
}
```

## Soft Delete

Use `active: false` for Teams and Players removed from a Competition. This prevents accidental loss of notes.

Provide a permanent delete option only in management screens.

