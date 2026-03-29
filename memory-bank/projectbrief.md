# VILD – Project Brief

> Last updated: 2026-03-29

## Overview

**VILD** (Vibration Interval Learning Device) is a two-module Android project that turns a paired Wear OS smartwatch (e.g. TicWatch) into a mindfulness vibration reminder, controlled from a companion phone app.

## Goals

1. Deliver random-interval vibration reminders on a Wear OS watch to support mindfulness / lucid-dreaming practice.
2. Provide a phone-side companion app that acts as the sole configuration UI — the watch runs headlessly.
3. Support multiple paired watches with the ability to target a specific watch or all watches simultaneously.

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
| Snooze until | Long (epoch ms) | Quick-snooze: 15 min / 30 min / 1 hr |
| Target node ID | String | Specific watch node ID or `"all"` |

## Communication

Phone → Watch communication uses the **Google Play Services Wearable Data Layer API** (`DataClient.putDataItem`). The Data Layer automatically delivers to all connected nodes and queues for offline nodes.

## Package Names

- Phone app: `com.example.vild`
- Wear app: `com.example.vild.wear`
- Shared library: `com.example.vild.shared`
