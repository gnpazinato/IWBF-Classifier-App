# Claude Code Start Prompt

Paste this into Claude Code after creating the repository and adding this planning pack.

```text
We are building IWBF Classifier App, a private Android tablet app for wheelchair basketball classification observation.

Read CLAUDE.md and all files in docs/ before coding.

The app must be native Android-first, Kotlin + Jetpack Compose, optimized for Samsung Galaxy Tab with S Pen. UI language is English and should use IWBF terminology.

Core MVP:

1. Local-first Competition records.
2. Import ZIP/DOCX/PDF roster documents, prioritizing DOCX tables.
3. Editable Teams and Players:
   - Team
   - Uniform Number
   - Player Name
   - Imported Sport Class
   - Sport Class Status
   - optional MIC fields
4. Manual blank competition flow for cases where no documents are provided.
5. Separate class fields:
   - Imported Sport Class
   - Starting Sport Class
   - My Opinion
   - Final Sport Class
6. Pen-first observation screen:
   - optional video area on top
   - handwritten S Pen note canvas below
   - fast Team/Player switching
   - autosave
7. Optional video evidence:
   - YouTube timestamp markers, not downloads
   - pending marker workflow for livestream delay
   - local MP4 attachments
8. Export Competition as ZIP.

Start by scaffolding the Android project and implementing Phase 1 from docs/08_mvp_build_plan.md.

Before implementation, propose the project structure and key data classes. Keep the first implementation small but real.
```

