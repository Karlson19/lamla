# Lamla

**Your day, considered.**

A native Android app for university students. Timetable, deadlines, study sessions, exam prep — quietly tracked, with reminders that actually fire on time. Offline-first. No accounts. No cloud. No telemetry.

[![Release](https://img.shields.io/github/v/release/yourusername/lamla)](../../releases/latest)
[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## Install (for end users)

1. Go to the [latest release](../../releases/latest).
2. Under "Assets", download `lamla-X.Y.Z.apk`.
3. On your Android phone, tap the downloaded file. If your phone warns about "Install unknown apps", grant the permission to whichever app delivered the APK (browser, Files, etc.) and tap the file again.
4. Open Lamla, walk through the 3-step onboarding, and start adding courses + classes.

**Requirements:** Android 8.0 (API 26) or newer.

### Privacy

- All data stays on your device. There are no servers, no analytics SDKs, no crash reporting.
- The app requests notifications, exact alarms, camera, microphone, and battery-optimization-exempt only because the features need them. Each one is opt-in via the standard Android dialog.
- No network calls except for downloading fonts (Inter, JetBrains Mono) the first time the app starts — and even those are cached for offline use afterwards.

### One small thing on Android quirks

If you're on Xiaomi, Huawei, Oppo, vivo, Samsung, Tecno, or Infinix: the OS may aggressively kill background apps, which can delay reminders. Open **Settings → Battery optimization** inside Lamla — you'll get manufacturer-specific instructions and a one-tap deep link to the right settings screen.

---

## Build from source

### Prerequisites

- **JDK 17** (Temurin or any other distribution)
- **Android SDK** with API 35 (compileSdk) — Android Studio handles this
- **Gradle 8.11+** — only needed once to bootstrap the wrapper jar; afterwards `./gradlew` works on its own

### First-time setup

```bash
git clone https://github.com/yourusername/lamla
cd lamla

# Bootstrap the gradle wrapper jar (only needed once):
#   Option A: open the project in Android Studio and let it sync.
#   Option B: with a system gradle installed, run `gradle wrapper`.

./gradlew assembleDebug
```

The unsigned debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Sideload it onto a phone or run from Android Studio.

### Notification sounds

Five `.ogg` sound files belong in `app/src/main/res/raw/`:

| File         | Used by                              |
|--------------|--------------------------------------|
| `chime.ogg`  | Class reminders                      |
| `urgent.ogg` | Deadline warnings                    |
| `alarm.ogg`  | Imminent deadlines (<1h to due)      |
| `ping.ogg`   | Study session start, office hours    |
| `bell.ogg`   | Exam alerts                          |

If any are missing, that channel falls back to the system default. The app still works correctly — just without distinct sounds per category.

Sources I'd recommend:
- [Freesound CC0 collection](https://freesound.org/browse/tags/cc0/)
- [Pixabay Sound Effects](https://pixabay.com/sound-effects/)

Encode with: `ffmpeg -i input.wav -c:a libvorbis -q:a 4 output.ogg`

---

## Releasing (for maintainers)

### One-time: generate a release keystore

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias lamla \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Keep the resulting `release.keystore` file safe. **Losing it means you can't ship updates that existing users can install over their current version** — Android requires every update to be signed by the same key.

### Set up CI signing

On GitHub: **Settings → Secrets and variables → Actions → New repository secret**. Add four secrets:

| Secret name                | Value                                                                  |
|----------------------------|------------------------------------------------------------------------|
| `LAMLA_KEYSTORE_BASE64`    | `base64 -w0 release.keystore` (Linux) or PowerShell `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))` |
| `LAMLA_KEYSTORE_PASSWORD`  | The store password you entered during keytool                          |
| `LAMLA_KEY_ALIAS`          | `lamla` (or whatever alias you chose)                                  |
| `LAMLA_KEY_PASSWORD`       | The key password (often the same as store password)                    |

### Cut a release

```bash
# Bump versionCode + versionName in app/build.gradle.kts
git add app/build.gradle.kts
git commit -m "Release v1.0.1"

# Tag the commit
git tag v1.0.1
git push origin main --tags
```

GitHub Actions (`.github/workflows/release.yml`) takes over from the tag push:

1. Spins up an Ubuntu runner
2. Decodes the keystore from your base64 secret
3. Builds a release APK with R8 minification + resource shrinking
4. Uploads `lamla-1.0.1.apk` as a release asset
5. Auto-generates release notes from commits since the last tag

You'll see the release at `https://github.com/yourusername/lamla/releases/tag/v1.0.1` within ~4-5 minutes. The APK is signed and ready for users to download.

### Build a release APK locally (without CI)

```bash
# Put your keystore at the repo root (gitignored):
cp ~/release.keystore .

# Copy and fill in keystore.properties (also gitignored):
cp keystore.properties.template keystore.properties
# Edit keystore.properties with your passwords

./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Tech

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (custom themed — not Material You purple)
- **Architecture:** MVVM + Clean (`data` / `domain` / `presentation` layers)
- **DI:** Hilt
- **DB:** Room (10 entities, autogenerated schemas)
- **Background:** AlarmManager (exact, for class/exam) + WorkManager (for reschedule jobs)
- **Preferences:** DataStore
- **Navigation:** Navigation Compose 2.8 with type-safe routes
- **Widgets:** Glance (Compose-style widgets)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35

## Improvements over the original spec

1. **Stress Score normalization** — original `Σ(weight × urgency)` is unbounded. We normalize to 0–100 via `tanh(Σraw / 50)` with exponential decay `e^(-h/48)`, plus a per-deadline breakdown sheet showing exact contributions.
2. **Text quick-capture** — added to the spec's photo + voice. It's the most common student capture type.
3. **Today's flow** — chronological interleaved timeline (Things 3 style) instead of separate "next class" + "deadlines today" cards.
4. **Office-hours surfacing** — 30 min before, the notification body lists your pending questions for that lecturer.
5. **Cancel-on-edit alarm hygiene** — `ReminderEngine` cancels old alarms before scheduling new ones on every mutation path. No leaks.
6. **Pomodoro: 4-cycle long break** — true Pomodoro technique, not just focus/short-break.
7. **Per-course accent color** — chips, timeline rails, study bar chart all inherit it.
8. **Glance widgets** instead of legacy RemoteViews.

## Design language

Not Material You purple. Not gradient bubbles. Reference points: **Things 3** (density + breathing room), **Linear** (utility + restraint), **Arc** (warm neutrals).

- **Warm off-white `#FAFAF7` / cool ink `#0B0B0E`** — not stark white/black
- **Inter via downloadable fonts** with **tabular figures** everywhere a number appears
- **JetBrains Mono only for the Pomodoro timer face**
- **1dp hairline borders + subtle bg-tint shifts** for depth — no heavy shadows
- **Spring physics** on state changes
- **5 themes**: System / Light / Dark / KNUST Gold / Monochrome

Open `ui-preview.html` in any browser to see the design language in action — six screens in light + dark mode with the exact tokens used in the Compose code.

## Architecture overview

```
app/src/main/java/app/lamla/
├── LamlaApplication.kt
├── di/                     — Hilt modules
├── data/
│   ├── local/              — Room entities, DAOs, Database, Converters, Mappers
│   ├── prefs/              — DataStore wrapper
│   └── repo/               — Repositories (per aggregate)
├── domain/
│   ├── model/              — pure domain types
│   └── usecase/            — StressScore, TodayFlow
├── notifications/          — Channels, AlarmScheduler, ReminderEngine,
│                             BootReceiver, ReminderReceiver, VoiceAnnouncer,
│                             RescheduleAllWorker, OemBatteryGuide
├── presentation/           — MainActivity, RootVM, Routes, NavHost, screens
├── widgets/                — Glance NextClass + TodaySummary widgets
└── ui/
    ├── theme/              — Color, Type, Shape, Spacing, Motion, Elevation
    └── components/         — Surface, Buttons, Chip, SectionHeader, EmptyState,
                              StressIndicator, FormFields, Hairline
```

## Non-negotiables

- **Offline-first.** No network calls anywhere except font downloads.
- **No telemetry, no analytics, no crash-reporting SDKs.**
- **Notification reliability.** Exact alarms for class/exam, boot-receiver reschedule, OEM-specific battery guides, no leaked alarms on edit.
- **ViewModels never touch Room directly.** Repositories enforce the boundary.

## License

[MIT](LICENSE).
