# VILD – Active Context

> Last updated: 2026-03-29T17:23 UTC-6

## Current State

The core architecture is implemented and functional. A **second wave of features** has been planned covering sync status, presets, day/night mode, snooze improvements, and a duration slider increase. See [`plans/new-features-plan.md`](../plans/new-features-plan.md) for the full implementation plan.

## What Was Recently Completed

- All core features from the first wave (vibrate now, vibration customization, custom snooze, snooze countdown).
- Watch-side Compose UI with "VILD is active" message and Test Vibration button.
- Build fixes (Gradle plugin, Wear Compose BOM, coroutines dependency).
- Architecture analysis and planning for the second wave of features.

## What's Next (Second Wave — Planned Features)

Five new features are planned — see [`plans/new-features-plan.md`](../plans/new-features-plan.md) for full details:

### Phase 1: Quick Wins
1. **Duration Slider Max → 4000ms** — Increase from 2000ms in [`VibrationSection.kt`](../app/src/main/java/com/example/vild/ui/VibrationSection.kt).
2. **Cancel Active Snooze** — Add a "Cancel Snooze" button in [`SnoozeSection.kt`](../app/src/main/java/com/example/vild/ui/SnoozeSection.kt) + `cancelSnooze()` in [`MainViewModel.kt`](../app/src/main/java/com/example/vild/MainViewModel.kt).

### Phase 2: Sync Status Indicator
3. **Sync Status** — Make [`WearSyncManager.pushSettings()`](../app/src/main/java/com/example/vild/data/WearSyncManager.kt) return success/failure. Show last sync time + status in the phone UI.

### Phase 3: Named Presets
4. **Presets** — Save/load/delete named presets. New [`Preset.kt`](../app/src/main/java/com/example/vild/data/Preset.kt) data class, JSON storage in DataStore, new [`PresetSection.kt`](../app/src/main/java/com/example/vild/ui/PresetSection.kt) UI.

### Phase 4: Day/Night Mode
5. **Day/Night Mode** — Toggle at the top of the screen. Two independent settings sets stored as JSON in DataStore. Presets can be loaded into either mode.

### Phase 5 (Prerequisite): Build Config
- Add `kotlinx-serialization-json` dependency for Preset and Day/Night JSON serialization.

### Implementation Order
1. Phase 5 (serialization dependency — prerequisite)
2. Phase 1 (duration slider + snooze cancel — quick wins)
3. Phase 2 (sync status indicator)
4. Phase 3 (named presets)
5. Phase 4 (day/night mode — builds on presets)

## Active Decisions

- The watch app has a minimal Compose UI but is still primarily headless — all configuration is done from the phone.
- Settings sync is **unidirectional** (phone → watch only) via Data Layer auto-sync.
- `AlarmManager.setAlarmClock()` is used for Doze-exempt alarm scheduling (no rate-limiting, supports short intervals like 1–2 min).
- `goAsync()` + coroutine pattern is used in `VibeReceiver`.
- **Immediate vibrate uses `MessageClient`** (fire-and-forget).
- **Custom snooze durations are phone-only** — the watch only receives the resulting `snooze_until_timestamp`.
- **Presets are phone-only** — the watch receives whatever settings are currently active, unaware of presets.
- **Day/Night mode is phone-only** — the watch receives whatever mode's settings are currently active.
- **Presets stored as JSON array** in a single DataStore key (avoids Room/SQLite complexity).
- **Day/Night settings stored as JSON** in two DataStore keys (`day_settings_json`, `night_settings_json`).
- **No changes to `:shared` or `:wear` modules** needed for the second wave — all new features are phone-side only.
- **Intensity max stays at 255** — this is an Android hardware limit for `VibrationEffect` amplitude.

## Known Issues

- None currently tracked.

---

### 2026-04-01T09:04 UTC-6 — Background redesign: parallax VILD icon on black

- Replaced the old full-screen `vild_background.webp` crop with a solid black background + the VILD icon (`vild_icon.png`) centered at 90% screen width, maintaining aspect ratio.
- The icon scrolls at 30% of the content scroll speed (parallax effect) using a shared `ScrollState` and `Modifier.offset { IntOffset(0, -(scrollState.value * 0.3f).toInt()) }`.
- Icon rendered at 40% alpha so it doesn't overpower the white text content.
- Old `vild_background.webp` is no longer referenced (can be deleted later).

## Recent Bug Fixes

### 2026-03-29T17:23 UTC-6 — Scheduled vibrations never repeating
- **Symptom:** Random vibrations fire once then never again, even with 1–2 min intervals.
- **Root cause 1 (Doze rate-limiting):** `setExactAndAllowWhileIdle()` has a system-enforced ~10 min minimum interval on API 31+. Short intervals were silently deferred/dropped.
- **Root cause 2 (Context issues):** `VibeReceiver` passed its short-lived BroadcastReceiver context to Play Services calls; 3s WakeLock was too short.
- **Fix:** Switched to `setAlarmClock()` (Doze-exempt, no rate-limiting). Also: `applicationContext` everywhere, 10s WakeLock, 5s Play Services timeout, emergency reschedule on failure.
