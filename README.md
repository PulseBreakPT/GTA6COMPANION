# LEONIDA ATLAS

Native independent, unofficial GTA VI companion for Android. Kotlin + Compose, Android 8.0+, target API 36. No WebView, account, advertising or analytics.

## Download an APK on Android

Open [Actions](https://github.com/PulseBreakPT/GTA6COMPANION/actions), choose a successful **Android · APK on every update** run, then **Artifacts → LeonidaAtlas-APK-…**. Download the ZIP, extract `app-debug.apk`, and allow installation from your browser/file manager when Android asks. A GitHub login may be required to download artifacts. Builds expire after 30 days; run the workflow again to regenerate them.

Every push and pull request runs the build. The APK is signed with a development key and intended for testing. The AAB artifact is an optimized **unsigned** release bundle; Play requires your upload signature. The separate **Signed Play release** workflow uses repository secrets and runs only on main when manually requested. It does not publish anything to Play.

## Build

Install JDK 21 and Android SDK platform 36/build-tools 36.0.0. Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=...`.

```sh
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:bundleRelease
./gradlew :app:connectedDebugAndroidTest :data:connectedDebugAndroidTest
```

Artifacts: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/bundle/release/app-release.aab`. See [publication guide](docs/PUBLISHING.md), [QA evidence](docs/QA.md), [architecture](docs/ARCHITECTURE.md), [privacy policy draft](store/privacy-policy.html).

## What works

Editorial home, date-based launch card, bilingual universal search, all encyclopedia categories, sourced dossier pages, linked official videos, news with read state, an editorial region atlas with gesture zoom and manual visits, per-profile favorites/history, collections, notes, objectives/planned dates, manual completion, goal calculator, comparison of confirmed dossiers, timeline, theme/language/shortcut preferences, story spoiler protection and JSON backup export/import with transactional merge. All bundled text works offline.

The 18 initial entries are deliberately conservative. Empty categories show an explicit verified-content empty state. The atlas is a non-geographic region index, not a map of invented coordinates. No Rockstar media is redistributed. Original native vector artwork is decorative and labeled as such.

## Publishing boundaries

This is an initial implementation, not a certified AAA product or a Play approval. See QA for executed checks and outstanding device coverage. Account-specific store contact details, privacy URL, upload signing credentials, IARC questionnaire and applicable closed testing must be completed by the account owner. No telemetry or push service is configured. Remote editorial sync is implemented but disabled until the owner installs a public trust key and configures an HTTPS signed feed. The app honestly reports this dependency and keeps its offline edition available.

## Content maintenance

Edit `content/content.json`, verify every claim against its source, bump the pack version, and update `verified`. Keep `app/src/main/assets/content.json` identical for bundled releases. `core:test` validates the content. For remote updates, run `scripts/setup_editorial_key.sh /private/path/key.pem` once; this installs only the public key in the app. Rebuild/distribute once to establish trust. Sign future JSON with `python3 scripts/sign_content.py content/content.json --private-key /private/path/key.pem --output signed-content.json`, host it on HTTPS, and configure that URL in app Settings. Signature, schema, version rollback, URL and size checks run before a transactional cache replacement. No scraping runs in the app. No public Rockstar API is assumed.

## Rights

The code and original vector art are supplied for this project. GTA VI, Rockstar Games and Take-Two marks and linked works belong to their owners. Source pages are linked for reference; no endorsement is implied. Complete a rights review of the app name, store listing and any later media before public distribution. Dependency notices are in `docs/DEPENDENCIES.md`.
