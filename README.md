# IWBF Classifier App - Planning Pack

This repository starter is a planning pack for a private Android app used by an IWBF wheelchair basketball classifier during competitions.

The app is not a generic note-taking app. It is a pen-first competition workflow for:

- importing team entry lists from IWBF-style Word/PDF documents;
- creating and correcting player records during a competition;
- writing handwritten observation notes with an S Pen;
- tracking imported class, starting class, classifier opinion, and final panel decision;
- attaching optional video evidence markers or local MP4 clips;
- exporting a competition package for backup or transfer.

The app UI must be in English and should use IWBF terminology where practical.

## Recommended Build Path

Build in two layers:

1. Native Android app first class target:
   - Kotlin
   - Jetpack Compose
   - AndroidX Ink API for stylus handwriting
   - Local file storage
   - Storage Access Framework for import/export

2. Optional web prototype:
   - Static web prototype can be hosted on GitHub Pages to validate layout and navigation.
   - The web prototype is not the final product because the core experience depends on S Pen handwriting, local Android files, and tablet ergonomics.

## Key Documents

- `CLAUDE.md`: project instructions for Claude Code.
- `docs/01_product_brief.md`: product purpose and core principles.
- `docs/02_competition_workflows.md`: real competition workflows.
- `docs/03_pen_first_ux.md`: S Pen and tablet UX requirements.
- `docs/04_data_model.md`: local data model.
- `docs/05_import_workflow.md`: DOCX/PDF/ZIP import behavior.
- `docs/06_video_workflow.md`: YouTube marker, delay, and local clip workflow.
- `docs/07_technical_architecture.md`: implementation plan.
- `docs/08_mvp_build_plan.md`: phased MVP checklist.
- `docs/09_iwbf_terms.md`: terminology to use in UI.
- `docs/10_claude_code_start_prompt.md`: pasteable prompt to start development.
- `docs/11_repository_setup.md`: GitHub, Codespaces, and GitHub Pages notes.
- `docs/samples/madrid_guadalajara_import_notes.md`: notes from the sample entry-list ZIP.

## Non-Goals

- No cloud account integration in the MVP.
- No app-managed Google Drive authentication in the MVP.
- No server database in the MVP.
- No automatic YouTube downloading or bypassing platform restrictions.
- No handwriting-to-text dependency for observation notes.
