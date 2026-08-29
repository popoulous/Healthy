# Healthy — Claude Code project rules

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication
- Talk to the user in Hungarian.
- Everything that lands in the repo is in English: code, identifiers,
  comments, commit messages, README.
- Exception, following Movora's practice: `IMPLEMENTATION_PLAN.md` and any
  future `PROJECT_CONTEXT.md` are written in Hungarian — they are working
  documents for the owner, not public-facing docs.
- The **app's own interface** is a separate matter from the repo language:
  base strings in `values/`, Hungarian in `values-hu/`. Every new string needs
  both, or lint fails the build on the missing translation.

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
A read-only Android dashboard over the owner's health data. Most of it comes
from Health Connect, which unifies what the vendor apps write (Mi Fitness →
watch, Zepp Life / Google Fit → scale).

Body composition is the exception and is **core, not an extra**: Zepp Life
writes only weight to Health Connect, so fat, muscle, bone and water are read
straight from the scale over BLE and computed here. See IMPLEMENTATION_PLAN.md
§15. "Read-only" still holds — nothing is ever written back to Health Connect,
including these.

## Hard constraints (do not violate without explicit approval)
- **Read-only.** The app never writes records to Health Connect.
- **On-device only.** No backend, no cloud, no analytics, no third-party
  network calls. Health data never leaves the phone.
- MVP scope = the metrics listed in the brief. Charts, long-range trends,
  export and home-screen widget are post-MVP — do not pull them forward.

## Stack (see IMPLEMENTATION_PLAN.md §3 for pinned versions)
- Kotlin, Jetpack Compose Material 3, single Gradle module.
- `androidx.health.connect:connect-client` 1.1.0 stable — not the 1.2.0 alphas.
- Package id `hu.galambos.healthy`. **minSdk 28**, compileSdk 37, targetSdk 36.
  The floor is 28, not 34, because the app also has to run on a Galaxy A71
  that stops at Android 13 — so the pre-Android-14 paths must stay working.
- DataStore Preferences for settings. Health Connect data is **never** mirrored
  locally — it is the database, and a copy would only buy a cache to
  invalidate. Room exists solely for scale measurements, which have no Health
  Connect home at all (IMPLEMENTATION_PLAN.md §15).
- No server component, no DI framework, no chart library (Compose `Canvas`),
  no navigation library — four flat tabs and one detail level are a nullable
  id and `BackHandler`. Adding a dependency needs a reason; the short list is
  part of the app's promise that nothing leaves the phone.
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

## Target devices
Two, and they differ in ways that matter:
- **Xiaomi 17T Pro, Android 16** — the real one, with the watch and scale data.
  Health Connect is in-platform.
- **Samsung Galaxy A71 (SM-A715F), Android 13** — the test device. Health
  Connect is an installable app here, and `READ_HEALTH_DATA_HISTORY` may not
  exist at all, so both paths must keep working.

A device that *cannot* grant history access is a different state from a user
who declined it. Keep them distinct — only the second is worth asking again.

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

The debug APK lands in `app/build/outputs/apk/debug/`; install it with the
SDK's own `adb` (`%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe install
-r`), which is not on PATH.

Screenshots for checking a change on a real phone: `adb shell screencap -p
/sdcard/s.png` then `adb pull`. Redirecting `exec-out` through PowerShell
corrupts the PNG; pulling a file does not. In Git Bash, `MSYS_NO_PATHCONV=1`
stops `/sdcard/...` being rewritten into a Windows path.

Gradle 9.7.1 via the wrapper; AGP 9.3.2 with built-in Kotlin, so **never apply
`org.jetbrains.kotlin.android`** — the Kotlin version is raised through the
root `buildscript` classpath instead.

## Lint is strict on purpose
`warningsAsErrors` is on. Three checks are disabled with reasons in
`app/build.gradle.kts`: `OldTargetApi`, `AndroidGradlePluginVersion` and
`GradleDependency` — a newer upstream release is news, not a defect, and
should not break a build the day it lands. Everything else is a real finding,
including unused resources: add a string in the same change that uses it.

## Not decided yet (ask, don't assume)
- Whether the project stays under `C:\xampp\htdocs` or moves out of the
  XAMPP web root.
- Whether background reads and a home-screen widget are wanted (v2).
