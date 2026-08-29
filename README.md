# Healthy

An Android app that reads Health Connect and shows everything it holds on one
screen. Read-only, on-device, no account and no network.

## Why it exists

A Xiaomi watch writes sleep, heart rate, blood oxygen, calories and steps into
Health Connect through Mi Fitness. A Xiaomi scale writes weight into it through
Zepp Life and Google Fit. Both end up in the same store, and no app shows both.
Healthy reads the store and puts it in one place.

## What it does

- Reads **33 record types** across activity, body, vitals, sleep, nutrition and
  wellness — every non-medical type Health Connect carries, minus cycle
  tracking, which nothing here writes.
- Shows each metric's newest reading with **the app that wrote it and when**,
  because a number without a source is not much use when four apps feed the
  same store.
- Draws a **7 or 30 day trend** per metric, and a detail screen with the range,
  the mean, and how the latest reading sits against it.
- Gives sleep its own screen: a hypnogram, the stage breakdown against clinical
  reference bands, the heart rate, blood oxygen and respiration recorded during
  the night, and a sleep score.
- Groups every reading by source app, so a missing metric is visibly a source
  that never shared it rather than a failure of this app.

## What it does not do

It never writes to Health Connect. It makes no network requests of any kind:
no account, no cloud, no analytics, no crash reporting. Nothing it stores is
included in a cloud backup or a device transfer. The dependency list is short
on purpose — Compose, the Health Connect client, DataStore, Lifecycle — so that
the claim is checkable rather than merely stated.

There is no database. Health Connect is the database; a copy would buy a cache
to invalidate and nothing else. Preferences live in DataStore.

## The sleep score is ours

Mi Fitness shows a sleep score and a percentile against an age group. Neither is
written to Health Connect, and the formula is not published, so this app does
not try to reproduce it — a number that looked like Xiaomi's but was not would
be worse than none.

Instead it computes its own, from arithmetic that can be checked: how close the
night was to a sensible length, how close each stage was to its reference share
(REM 10–30%, light 20–60%, deep 20–40%), and a penalty for waking repeatedly.
The screen says whose score it is. Where a source recorded only "asleep" without
stages, there is no score at all rather than one invented from a duration.

## Requirements

- Android 9 or newer. Health Connect is part of the platform from Android 14; on
  Android 9–13 the app prompts to install it.
- **Reading other apps' data past 30 days needs `READ_HEALTH_DATA_HISTORY`,**
  which the app requests on first run. The 30-day window is pinned to the first
  grant rather than sliding, and uninstalling the app resets it — so granting
  that permission late costs the months in between, permanently. Older Health
  Connect versions do not offer it at all, in which case the app says so and
  shows the last month.
- Permissions are per type. Refusing some is normal; those metrics are simply
  left off the dashboard.

## Building

The JDK on `PATH` may be newer than AGP supports; use the one bundled with
Android Studio.

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
```

Kotlin and Compose, one module, AGP 9 with built-in Kotlin — so the
`org.jetbrains.kotlin.android` plugin must **not** be applied.

## Licence

MIT. See [LICENSE](LICENSE).
