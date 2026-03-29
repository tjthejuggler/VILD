# VILD – Project Brief

> Last updated: 2026-03-29T15:18 UTC-6

## Overview

**VILD** (Vibration Interval Learning Device) is a two-module Android project that turns a paired Wear OS smartwatch (e.g. TicWatch) into a mindfulness vibration reminder, controlled from a companion phone app.

## Goals

1. Deliver random-interval vibration reminders on a Wear OS watch to support mindfulness / lucid-dreaming practice.
2. Provide a phone-side companion app that acts as the sole configuration UI — the watch runs headlessly.
3. Support multiple paired watches with the ability to target a specific watch or all watches simultaneously.
4. Allow full vibration customization (intensity, duration, pattern, repeat count) with an immediate test button.
5. Provide flexible snooze options including user-created custom durations and a live countdown display.

## Modules

| Module | Role |
|--------|------|
| `:app` | Phone companion — Jetpack Compose UI for configuring settings |
| `:wear` | Wear OS worker — receives settings, schedules alarms, fires vibrations |
| `:shared` | Kotlin library — single source of truth for Data Layer paths and keys |

## Key Settings

| Setting | Type | Range / Notes |
|---------|------|---------------|
| Enabled | Boolean | Master on/off |
| Min interval | Int (minutes) | 1–120 |
| Max interval | Int (minutes) | 1–120, ≥ min |
| Vibration intensity | Int | 1–255 |
| Vibration duration | Long (ms) | 100–2000 (planned) |
| Vibration pattern | String | single / double / triple / ramp (planned) |
| Vibration repeat count | Int | 1–5 (planned) |
| Snooze until | Long (epoch ms) | Quick-snooze: 15 min / 30 min / 1 hr + custom |
| Custom snooze durations | List of Long (ms) | User-created, phone-only (planned) |
| Target node ID | String | Specific watch node ID or `"all"` |

## Communication

- **Settings sync**: Phone → Watch via **Google Play Services Wearable Data Layer API** (`DataClient.putDataItem`). Automatic delivery to all connected nodes; queued for offline nodes.
- **Immediate vibrate**: Phone → Watch via **MessageClient** (`sendMessage`). Fire-and-forget; requires watch to be connected. (Planned)

## Package Names

- Phone app: `com.example.vild`
- Wear app: `com.example.vild.wear`
- Shared library: `com.example.vild.shared`
