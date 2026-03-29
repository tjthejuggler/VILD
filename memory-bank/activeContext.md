# VILD – Active Context

> Last updated: 2026-03-29T15:15 UTC-6

## Current State

The core architecture and logic for all three modules (`:app`, `:wear`, `:shared`) have been implemented. The project is in a **functional baseline** state — all primary features are coded and wired together. A new feature planning phase has begun.

## What Was Recently Completed

- `:shared` module with `VibeConstants` (Data Layer path and all keys).
- `:wear` module with full vibration pipeline:
  - `VibeSettingsRepository` — SharedPreferences storage on the watch.
  - `VibeDataListenerService` — receives Data Layer updates, persists settings, triggers scheduler.
  - `VibeScheduler` — AlarmManager scheduling with target-node filtering and snooze support.
  - `VibeReceiver` — BroadcastReceiver that fires vibration and reschedules.
- `:app` module with full phone companion:
  - `AppSettingsRepository` — DataStore Preferences for local persistence.
  - `WearSyncManager` — pushes settings to Data Layer with `setUrgent()`.
  - `MainViewModel` — AndroidViewModel coordinating repo + sync.
  - `MainActivity` — Jetpack Compose UI with all controls.
- Minimal wear-side `MainActivity` added for Android Studio run configuration support (calls `finish()` immediately).

## What's Next (Planned Features)

Four new features are planned — see [`plans/new-features-plan.md`](../plans/new-features-plan.md) for full details:

1. **Immediate Vibrate Button** — Phone sends a one-shot vibration command to the watch via `MessageClient.sendMessage()`. Uses the currently configured intensity and pattern.
2. **Vibration Customization** — New settings for vibration duration (ms), pattern type (single/double/triple/ramp), and repeat count. Synced to watch via Data Layer. Extracted into a shared `VibrationHelper` on the watch.
3. **Custom Snooze Buttons** — Users can create/save/delete custom snooze durations beyond the hardcoded 15/30/60 min. Stored in phone-side DataStore only.
4. **Snooze Status Display** — Live countdown on the phone showing remaining snooze time.

### Implementation Order
1. Add new constants to `:shared` (`VibeConstants`)
2. Update `VibeSettings` data class and `AppSettingsRepository`
3. Update `WearSyncManager` (new DataMap fields + `sendVibrateNow()` via MessageClient)
4. Update watch-side `VibeSettingsRepository` (new field getters)
5. Create `VibrationHelper` on watch (extracted vibration logic with pattern support)
6. Update `VibeReceiver` to delegate to `VibrationHelper`
7. Update `VibeDataListenerService` (new fields + `onMessageReceived()` for vibrate-now)
8. Update `MainViewModel` (new update methods, vibrateNow, custom snooze, countdown)
9. Extract phone UI into `VibrationSection.kt` and `SnoozeSection.kt`
10. Update `wear/AndroidManifest.xml` (add MESSAGE_RECEIVED intent filter)
11. Update README and memory bank

## Active Decisions

- The watch app is **headless** — no Activity UI, only services and receivers. The launcher Activity calls `finish()` immediately.
- Settings sync is **unidirectional** (phone → watch only).
- `AlarmManager.setExactAndAllowWhileIdle()` is used for Doze-mode reliability.
- `goAsync()` + coroutine pattern is used in `VibeReceiver` to allow suspend calls within the broadcast receiver lifecycle.
- **Immediate vibrate uses `MessageClient`** (fire-and-forget) rather than `DataClient` (persistent state).
- **Vibration logic will be extracted** into a `VibrationHelper` shared by both `VibeReceiver` and the message handler.
- **Custom snooze durations are phone-only** — the watch only receives the resulting `snooze_until_timestamp`.
- **`onMessageReceived()` will be merged** into the existing `VibeDataListenerService` rather than creating a separate service.

## Known Issues

- None currently tracked. The project has just reached its initial implementation milestone.
