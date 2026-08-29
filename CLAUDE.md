# Healthy — Claude Code project rules

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication
- Talk to the user in Hungarian.
- Everything that lands in the repo is in English: code, identifiers,
  comments, commit messages, docs, README.

## Workflow
- Never guess — look things up (codebase, official Android / Health Connect
  docs) before acting. Health Connect APIs and permission rules change per
  Android release; verify against the current SDK docs, do not recall.
- Discuss the approach first, produce a plan, and only start coding after
  the user approves it. Use Plan Mode for any multi-file or risky change.
- The product brief lives in `health-connect-dashboard-brief.md` — read it
  before planning. The detailed design will live in `IMPLEMENTATION_PLAN.md`.
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

## Stack
- Kotlin, Jetpack Compose, single Android module.
- `androidx.health.connect:connect-client` (current stable — verify version).
- No server component.

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

## Not decided yet (ask, don't assume)
- App name and package id.
- Whether the project stays under `C:\xampp\htdocs` or moves out of the
  XAMPP web root.
- Build/lint/test commands: no Gradle project exists yet. Fill this file's
  command section in once the scaffold is generated — do not invent them.
