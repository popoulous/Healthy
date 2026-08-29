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
- **Reads the scale directly over Bluetooth**, because Zepp Life writes only
  weight to Health Connect and keeps body fat, muscle, bone and water to
  itself. The scale broadcasts weight and a raw impedance in the clear; the
  composition is worked out here.
- Keeps its own history, so a trend survives Health Connect's auto-delete —
  which is the phone owner's setting, not this app's.

## What it does not do

It never writes to Health Connect. It makes no network requests of any kind:
no account, no cloud, no analytics, no crash reporting. Nothing it stores is
included in a cloud backup or a device transfer. The dependency list is short
on purpose — Compose, the Health Connect client, Room, DataStore, Lifecycle —
so that the claim is checkable rather than merely stated.

It never talks *to* the scale either. There is no pairing and no connection:
the scale broadcasts its measurement to the room and this app listens. The
Bluetooth scan permission is declared `neverForLocation`, so Android does not
also demand location access — and below Android 12, where a scan requires
location regardless, the feature declines to offer itself rather than ask.

Raw Health Connect records are not copied. Health Connect is their database,
and syncing is incremental through its Changes API. What is stored locally is
what the screen draws — a value per metric per day and the newest reading of
each — plus the scale measurements, which have no home in Health Connect at
all.

## The body composition figures are computed here

The scale weighs you and measures the impedance of a small current; it computes
no body composition at all. Zepp Life works that out in software and shares
only the weight.

So this app works it out too, from the Mi Fit algorithm as reconstructed by the
open-source community — the only published description of it. Expect small
disagreements with what Zepp Life shows: the current formula is not public, and
no amount of care here can close that gap. Only the raw weight and impedance
are stored, so correcting your height or year of birth re-derives every past
measurement rather than leaving behind figures computed for a different person.

Height, year of birth and sex are needed for the formula. Without them the app
records the weight and leaves the rest alone.

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
- Reading the scale needs Android 12 or newer and a Mi Body Composition Scale 2
  in range. It listens only while you ask it to, from the settings screen.

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
