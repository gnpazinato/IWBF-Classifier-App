# Repository Setup Notes

Suggested repository name:

`iwbf-classifier-app`

Suggested display name:

`IWBF Classifier App`

## Recommended Initial Repository Layout

```text
iwbf-classifier-app/
  README.md
  CLAUDE.md
  docs/
  app/
  prototype-web/
  .github/
    workflows/
```

## Codespaces

Codespaces is a good fit because it allows development from the browser without installing Android tooling locally.

Recommended devcontainer contents:

- Java JDK compatible with current Android Gradle Plugin.
- Android SDK command-line tools.
- Gradle cache.
- Node.js only if building a web prototype.

Claude Code can be used inside the codespace after the repository is created.

## GitHub Pages

GitHub Pages can host only the optional static prototype.

Use it for:

- validating the observation screen layout;
- testing roster navigation;
- showing import review mockups;
- sharing screenshots or flows.

Do not use it as the production app target.

Production target remains native Android because of:

- S Pen behavior;
- local file storage;
- video attachments;
- export/import;
- tablet-specific performance.

## Suggested Development Order

1. Add this planning pack to the repository.
2. Ask Claude Code to scaffold the Android project.
3. Implement local data model and manual Competition/Team/Player flow.
4. Add DOCX import.
5. Add S Pen note canvas.
6. Add optional video markers.
7. Add export ZIP.

## First Claude Code Task

Start small:

```text
Create a Kotlin/Jetpack Compose Android project for IWBF Classifier App.
Implement local models for Competition, Team, Player, Game, ObservationEvent, VideoMarker, and Attachment.
Create screens for Competition list, Competition detail, Team roster, and Player edit.
Use in-memory repository first, but design the repository interface so it can be backed by local JSON files next.
Follow CLAUDE.md and docs/.
```

