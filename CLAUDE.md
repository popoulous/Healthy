# Healthy — Claude Code project rules

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication
- Talk to the user in Hungarian.
- Everything that lands in the repo is in English: code, identifiers,
  comments, commit messages, README.
- Exception, following Movora's practice: `IMPLEMENTATION_PLAN.md` and any
  future `PROJECT_CONTEXT.md` are written in Hungarian — they are working
  documents for the owner, not public-facing docs.

## Workflow
- Never guess — look things up (codebase, official Android / Health Connect
  docs) before acting. Health Connect APIs and permission rules change per
  Android release; verify against the current SDK docs, do not recall.
- Discuss the approach first, produce a plan, and only start coding after
  the user approves it. Use Plan Mode for any multi-file or risky change.
- The product brief lives in `health-connect-dashboard-brief.md`, the visual
  direction in `design.txt` (authoritative) and `docs/design-mockup.png`
  (illustration); the plan and the reasoning behind every decision live in
  `IMPLEMENTATION_PLAN.md`. Read them before planning. The plan deliberately overrides the brief in
  places (minSdk, scope) — the plan wins, and §2 says why.
- Keep this file concise; record project-level decisions in memory.
- The developer is experienced (13+ years) — skip Kotlin/Android basics,
  focus on the Health Connect specifics.

## What this app is
Read-only Android dashboard over Health Connect: it unifies data written by
different vendor apps (Mi Fitness → watch, Zepp Life / Google Fit → scale)
into one screen. See the brief for the metric list.

## Hard constraints (do not violate without explicit approval)
- **Read-only.** The app never writes records to Health Connect.
- **On-device only.** No backend, no cloud, no analytics, no third-party
  network calls. Health data never leaves the phone.
- MVP scope = the metrics listed in the brief. Charts, long-range trends,
  export and home-screen widget are post-MVP — do not pull them forward.

## Stack (see IMPLEMENTATION_PLAN.md §3 for pinned versions)
- Kotlin, Jetpack Compose Material 3, single Gradle module.
- `androidx.health.connect:connect-client` 1.1.0 stable — not the 1.2.0 alphas.
- Package id `hu.galambos.healthy`. minSdk 34, compileSdk 37, targetSdk 36.
- DataStore Preferences for settings. **No database** — Health Connect is the
  database; a local mirror would only buy a cache-invalidation problem.
- No server component, no DI framework, no chart library (Compose `Canvas`).
- React Native was considered and rejected: Health Connect is Android-only, so
  there is no cross-platform win, and the RN wrapper covers ~40 of 62 types.

## Architecture (see IMPLEMENTATION_PLAN.md §4)
- Everything derives from one `MetricDescriptor` registry: the permission set,
  the read strategy, and the dashboard cards. Adding a data type is one row
  there — never a new hand-written card.
- `HealthRepository` is the stable interface; `data/hc/` is the only package
  allowed to know the Health Connect SDK. A `FakeRepository` backs previews
  and unit tests, because an emulator has no real health data to read.
- Reading all data over long ranges only works via the aggregate APIs. Raw
  `readRecords` is for the latest record only (source app + timestamp).

## Target device
Xiaomi 14T Pro on Android 16. Health Connect is in-platform (Android 14+), so
the legacy "install Health Connect from Play" path does not exist here.
Reading other apps' data past 30 days requires `READ_HEALTH_DATA_HISTORY`,
requested on first run — see IMPLEMENTATION_PLAN.md §5.

## Local environment (verified)
- Android Studio: `C:\Program Files\Android\Android Studio` (build 2026.1).
- SDK: `C:\Users\galam\AppData\Local\Android\Sdk` — platforms `android-35`,
  `android-36.1`; build-tools up to `37.0.0`; `platform-tools\adb.exe` present.
- `adb` and `gradle` are NOT on PATH; use the SDK path and the Gradle wrapper.
- PATH `java` is JDK 26, which AGP does not support — build with the JBR
  bundled in Android Studio (`...\Android Studio\jbr`) via `JAVA_HOME`.

## Quality
- Conventional Commits. Subject in the imperative, body in prose explaining
  why the change exists — no bullet lists of what changed, no co-author or
  session trailers.
- Remote: https://github.com/popoulous/Healthy.git (public — the history is
  part of the work product).

## Build

Set `JAVA_HOME` first — the JDK on PATH is 26, which AGP rejects:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug          # debug APK
.\gradlew.bat lintDebug              # lint; warningsAsErrors is on
.\gradlew.bat clean lintDebug assembleDebug
```

There are no tests yet — they arrive in F2 with the first repository. The
debug APK lands in `app/build/outputs/apk/debug/`; install it with the SDK's
own `adb` (`%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe install -r`),
which is not on PATH.

Gradle 9.7.1 via the wrapper; AGP 9.3.2 with built-in Kotlin, so **never apply
`org.jetbrains.kotlin.android`** — the Kotlin version is raised through the
root `buildscript` classpath instead.

## Not decided yet (ask, don't assume)
- Whether the project stays under `C:\xampp\htdocs` or moves out of the
  XAMPP web root.
- The three items in IMPLEMENTATION_PLAN.md §11.
