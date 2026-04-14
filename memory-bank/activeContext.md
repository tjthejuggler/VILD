# VILD – Active Context

> Last updated: 2026-04-13T12:23 UTC-4

## Current State

The core architecture is implemented and functional. The **daily reality check trigger notification** feature has been added — users can manage a list of reality check triggers in Settings, and one is randomly chosen each morning at 8 AM and shown as a notification.

## What Was Recently Completed

- All core features from the first wave (vibrate now, vibration customization, custom snooze, snooze countdown).
- Watch-side Compose UI with "VILD is active" message and Test Vibration button.
- Build fixes (Gradle plugin, Wear Compose BOM, coroutines dependency).
- Architecture analysis and planning for the second wave of features.
- **Daily reality check trigger notification** (2026-04-13).

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

### 2026-04-13T12:23 UTC-4 — Daily reality check trigger notification

- Created [`RealityCheckTrigger`](../app/src/main/java/com/example/vild/data/RealityCheckTrigger.kt) data class (same pattern as `AdviceItem`).
- Created [`RealityCheckRepository`](../app/src/main/java/com/example/vild/data/RealityCheckRepository.kt) — DataStore JSON persistence with add/update/delete.
- Created [`NotificationHelper`](../app/src/main/java/com/example/vild/data/NotificationHelper.kt) — notification channel creation + showing the daily trigger notification.
- Created [`DailyTriggerScheduler`](../app/src/main/java/com/example/vild/data/DailyTriggerScheduler.kt) — AlarmManager `setAlarmClock()` at 8 AM daily.
- Created [`DailyTriggerReceiver`](../app/src/main/java/com/example/vild/ipc/DailyTriggerReceiver.kt) — picks random trigger, shows notification, reschedules for next day.
- Created [`BootReceiver`](../app/src/main/java/com/example/vild/ipc/BootReceiver.kt) — reschedules daily alarm after device reboot.
- Created [`RealityCheckDialog`](../app/src/main/java/com/example/vild/ui/realitycheck/RealityCheckDialog.kt) — UI for managing triggers (same pattern as `AdviceDialog`).
- Updated [`MainViewModel`](../app/src/main/java/com/example/vild/MainViewModel.kt) — added `triggerRepo`, `triggers` StateFlow, `addTrigger()`, `updateTrigger()`, `deleteTrigger()`, and scheduling on init.
- Updated [`SettingsScreen`](../app/src/main/java/com/example/vild/ui/settings/SettingsScreen.kt) — added `RealityCheckTriggersCard` and wired trigger CRUD callbacks.
- Updated [`MainActivity`](../app/src/main/java/com/example/vild/MainActivity.kt) — added POST_NOTIFICATIONS permission request (Android 13+), wired trigger state and callbacks to SettingsScreen.
- Updated [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) — added `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` permissions; registered `DailyTriggerReceiver` and `BootReceiver`.
- Build successful.

---

### 2026-04-02T23:22 UTC-6 — Advice notes feature

- Added `notes: String = ""` field to [`AdviceItem`](../app/src/main/java/com/example/vild/data/AdviceItem.kt) (backward-compatible default).
- Added `updateNotes(id, notes)` to [`AdviceRepository`](../app/src/main/java/com/example/vild/data/AdviceRepository.kt).
- Added `updateAdviceNotes(id, notes)` to [`MainViewModel`](../app/src/main/java/com/example/vild/MainViewModel.kt).
- Created [`AdviceNotesDialog`](../app/src/main/java/com/example/vild/ui/advice/AdviceNotesDialog.kt) — a popup composable showing the advice text and a multi-line text field for personal notes. Save persists; Cancel discards.
- Added `onTap: (AdviceItem) -> Unit` callback to [`AdviceBanner`](../app/src/main/java/com/example/vild/ui/advice/AdviceBanner.kt) using `combinedClickable`.
- Wired `onTap` in [`MainActivity`](../app/src/main/java/com/example/vild/MainActivity.kt): tapping the banner opens `AdviceNotesDialog` for the currently shown advice item.
- Build successful, installed on device.

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
