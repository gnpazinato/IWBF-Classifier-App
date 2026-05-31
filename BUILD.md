# Building & Running

Native Android (Kotlin + Jetpack Compose). Developed entirely in the cloud
(GitHub Codespaces); tested by installing the debug APK on the Samsung Galaxy Tab.

## Stack

- Kotlin 2.1, Jetpack Compose (Material 3), single `:app` module
- Manual DI (`AppContainer`), kotlinx.serialization, Coroutines/Flow
- AGP 8.7.3, Gradle 8.11.1 (wrapper), **JDK 17 required**, compileSdk 35, minSdk 26
- Local JSON storage under `filesDir/competitions/...` (docs/04 layout)

## Build in the Codespace (no local install)

The Codespace's default JDK is 25, but the Android Gradle Plugin needs JDK 17.
A one-time provisioning script installed JDK 17 + the Android SDK; load that env
before building:

```bash
source /tmp/android_env.sh          # sets JAVA_HOME (17), ANDROID_HOME, PATH
./gradlew assembleDebug             # outputs app/build/outputs/apk/debug/app-debug.apk
```

If `/tmp/android_env.sh` is gone (fresh Codespace), JDK 17 + the SDK can be
reinstalled; CI (below) is the canonical build path.

## Install on the Samsung tablet

1. Build `app-debug.apk` (locally as above, or download the **app-debug**
   artifact from the GitHub Actions run).
2. Open the APK on the tablet (Drive / direct download) and allow install from
   unknown sources.
3. Launch **IWBF Classifier App** (landscape).

## CI

`.github/workflows/android.yml` builds `assembleDebug` on every push/PR with
JDK 17 + SDK 35 and uploads the debug APK as an artifact — the source of truth
for "does it build", and the easiest way to get an APK onto the tablet.

## Phase 1 scope (this build)

Competition create/list/edit/delete · manual Team & Player CRUD · editable
imported class / SCS / decision fields (Starting, My Opinion, Final) / MIC ·
soft delete + restore (archive) · local JSON persistence · design system ·
observation screen scaffold (S Pen ink lands in Phase 3). Import (Phase 2),
ink canvas (Phase 3), video (Phase 5–6) and ZIP export (Phase 7) are stubbed.
