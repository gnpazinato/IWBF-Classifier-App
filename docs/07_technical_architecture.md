# Technical Architecture

## Target Platform

Primary target:

- Android tablet
- Landscape orientation
- S Pen / stylus support

Recommended stack:

- Kotlin
- Jetpack Compose
- AndroidX Ink API for handwriting
- WebView or external YouTube player integration for optional YouTube marker mode
- Storage Access Framework for import/export
- Local JSON files for app data

## Why Native Android

The core experience depends on:

- low-latency handwriting;
- distinguishing stylus from finger;
- local file import/export;
- MP4 attachments;
- tablet-specific layout;
- offline use.

A static web app can prototype layout, but it should not be considered the final product.

## Optional Web Prototype

GitHub Pages can host a static prototype for:

- navigation testing;
- roster import mockups;
- visual layout;
- user flow review.

Limitations:

- not a true Android app;
- weaker stylus ergonomics;
- no native local file package management;
- no reliable Android MP4 attachment workflow;
- no final S Pen performance validation.

## Modules

Suggested Android modules or packages:

```text
app/
  ui/
    competition/
    importreview/
    roster/
    observation/
    playerdetail/
    discussion/
    export/
  data/
    model/
    repository/
    storage/
    importers/
    export/
  ink/
    canvas/
    serialization/
    preview/
  video/
    youtube/
    markers/
    attachments/
  domain/
    competition/
    classification/
    observation/
```

## Storage Approach

Use local app-specific storage for active working data.

Use Storage Access Framework for:

- importing ZIP/DOCX/PDF/MP4;
- exporting competition ZIP;
- selecting a backup destination;
- sharing to Drive or MacBook without app-managed authentication.

## Import Libraries

Android-native parsing may be limited. Options:

1. Parse DOCX on-device using a Java/Kotlin-compatible ZIP/XML approach.
2. Use Apache POI carefully if dependency size is acceptable.
3. For MVP, support a narrow DOCX table parser for common IWBF-style files.
4. PDF parsing should be best effort.

Recommended MVP:

- DOCX ZIP/XML table extraction for entry lists.
- Best-effort PDF text extraction later.

## Ink Storage

Do not rely only on screenshots of notes.

Store:

- vector/stroke data for future editing;
- rendered PNG preview for quick browsing and PDF export.

Ink data should preserve:

- points;
- pressure if available;
- stroke color;
- stroke width;
- tool type;
- page/canvas dimensions.

## Export

Competition export should create a ZIP with:

- all JSON metadata;
- ink files;
- preview PNGs;
- import report;
- local MP4 attachments;
- optional generated PDF summaries.

Do not include YouTube video downloads. Include YouTube markers as JSON and, optionally, clickable links.

## Security and Privacy

The app is local-first but may contain sensitive medical/classification notes.

Recommendations:

- no analytics in MVP;
- no external network except user-provided YouTube playback;
- no automatic upload;
- app-specific internal storage for working data;
- explicit export action for sharing.

## Testing Priorities

1. Stylus latency and palm rejection feel.
2. Autosave reliability.
3. Player switching speed.
4. Import from sample DOCX ZIP.
5. Editing data after import.
6. Export/import round trip.
7. Video marker linking with a delayed livestream or VOD.

