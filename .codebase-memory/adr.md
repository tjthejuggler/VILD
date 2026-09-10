# ADR — Night vibes end at a configured wall-clock time; per-pulse markers are radio buttons

**Date:** 2026-09-10

## Context
Users received "Are you dreaming? Look at your hands." notifications during the daytime.
Root cause: `NightWindow.resolve` ended the night window a full 24 h after its start
(`morningEnd = start.plusDays(1)`), so REM-phase pulses kept firing until the user manually
toggled Day mode. Separately, the per-pulse experience markers (Noticed / In dream / Woke me)
were independent toggles even though the states are mutually exclusive.

## Decision
1. The night window now ends at a user-configurable `NightVibeSettings.nightEndMinutes`
   (default 08:00, persisted as `night_end_minutes` in DataStore). `NightWindow.resolve`
   clamps `morningEnd` to this time (handling the midnight wrap) and clamps `gapEnd` to it.
   Night vibes therefore can never fire after the configured end — daytime notifications
   are exclusively the daily reality-check trigger (`DailyTriggerReceiver` + `NagScheduler`
   posting `log.triggerText`).
2. Per-pulse markers became a radio choice via the `NightVibeMark` enum
   (`UNNOTICED` default / `IN_DREAM` / `WOKE_ME`) with `mark`/`withMark` helpers.
   UI (Settings `NightVibeLogSection` and Night-chart `EntryRow`) exposes one-choice chips.
   The boolean storage (`noticed`/`inDream`/`wokeMeUp`) is unchanged for backward
   compatibility; legacy `noticed=true` entries read as `IN_DREAM`. The chart's
   gold "noticed" segment still counts `entry.noticed`.

## Consequences
- The old "night has no set end" behavior is gone; the Day/Night toggle no longer gates
  whether vibes stop in the morning.
- `nightEndMinutes` participates in day/night mode snapshots (`saveModeSettings`) since it
  lives inside `NightVibeSettings`.
- Existing stored entries need no migration.