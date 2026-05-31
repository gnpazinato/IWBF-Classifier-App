# Video Workflow

Video must be optional. The app must work perfectly without a livestream.

## Video Modes

### No Video

User hides the video panel. The handwritten note canvas expands.

Use when:

- competition has no livestream;
- Wi-Fi is poor;
- user wants paper-like focus.

### YouTube Marker Mode

The app embeds or opens a YouTube player and stores video timestamps as evidence markers.

The app does not download YouTube video.

Stored data:

- YouTube video ID;
- start time;
- end time;
- playback speed;
- player;
- label;
- linked observation event.

### Local Clip Mode

User attaches a local MP4 file to a Player.

The MP4 can come from:

- Samsung/Android screen recorder;
- authorized event video file;
- video shared by the competition;
- imported local clip.

Future version:

- trim local MP4 to 5-15 seconds;
- compress to 720p;
- remove audio by default.

## Real-Time Livestream Delay

In a venue, the YouTube livestream often lags behind the live action.

The app should not assume the current YouTube time matches what the classifier just saw live.

Use one of two workflows.

## Workflow A - Fast Manual Link

This is the safest MVP workflow.

1. User sees an important movement live.
2. User selects the Player.
3. User taps `Flag Moment`.
4. App creates a pending Observation Event with:
   - player;
   - device time;
   - optional game period/clock;
   - current handwritten page;
   - status: `Pending Video Link`.
5. User keeps watching the game and writing.
6. When the delayed YouTube video reaches that movement, user taps `Link Current Video`.
7. App reads the current YouTube time and creates a clip marker:
   - start = current time minus 5 seconds;
   - end = current time plus 10 seconds;
   - playback = 0.5x by default.
8. The pending event becomes linked.

Advantages:

- no need to know exact delay;
- very low risk;
- works with variable stream delay;
- one tap when live, one tap when video catches up.

## Workflow B - Calibrated Delay

This is a helpful enhancement after MVP.

1. User taps `Sync Live` at a recognizable live event, such as whistle, jump ball, made basket, scoreboard change.
2. When the same event appears in the YouTube video, user taps `Sync Video`.
3. App calculates stream delay.
4. Later, when user taps `Flag Moment`, app estimates when the movement will appear in video.
5. App shows a small pending prompt after the delay:
   - `#8 pending clip now`
6. User taps confirm or link current video.

The app should still require user confirmation because livestream delay can drift.

## Suggested Controls

Video panel controls:

- `Load Link`
- `Hide Video`
- `Flag Moment`
- `Link Current Video`
- `Replay Last Marker`
- `-5s`
- `+5s`
- `0.25x`
- `0.5x`
- `1.0x`

Player event panel:

- Pending Video Link
- Linked YouTube Marker
- Local Clip Attached

## Example Real Game Flow

Game: Spain vs France.

Selected Player: `#20 WEMBOLUA`.

1. Live action: player reaches forward after contact and shows trunk behavior relevant to class decision.
2. User taps `Flag Moment`.
3. User writes: freehand note on the Player canvas.
4. The app creates:
   - Observation Event: `#20`, time now, `Pending Video Link`.
5. About 35 seconds later, the movement appears in the YouTube panel.
6. User taps `Link Current Video`.
7. App creates marker:
   - YouTube ID: current game video;
   - start: current player time - 5 seconds;
   - end: current player time + 10 seconds;
   - speed: 0.5x;
   - label: optional.
8. Later in panel discussion, user opens Player #20 and taps the marker to replay the exact section.

## Screen Recording Option

The app should allow attaching a local MP4, but in-app screen recording should not be a blocking MVP feature.

Practical MVP:

1. User uses Samsung/Android screen recorder if needed.
2. User trims the recording with system/gallery tools if desired.
3. User attaches the MP4 to the Player.
4. App copies the MP4 into the competition archive.

Why:

- Android screen capture requires user consent.
- Capturing audio from other apps has restrictions.
- Some protected content may not record correctly.
- YouTube content should not be downloaded by the app.

## Storage Guidance

Keep clips short:

- target length: 5-15 seconds;
- target resolution: 720p for app-created local clips;
- audio off by default;
- allow deleting clips after final decision.

Estimated storage:

- 10-15 seconds at 720p may be roughly 5-20 MB depending on encoding.
- 50 clips can become several hundred MB.
- Full-resolution screen recordings can be much larger.

Add management tools:

- show storage used by competition;
- delete selected clips;
- delete all clips for finalized players;
- export before delete.

