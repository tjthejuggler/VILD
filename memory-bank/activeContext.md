# VILD – Active Context

> Last updated: 2026-03-29

## Current State

The core architecture and logic for all three modules (`:app`, `:wear`, `:shared`) have been implemented. The project is in a **functional baseline** state — all primary features are coded and wired together.

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

## What's Next (Potential)

- Testing on physical devices (phone + Wear OS watch pairing).
- Error handling / edge cases (e.g. exact alarm permission prompts on Android 12+).
- Possible watch-side UI for status display.
- Possible notification channel for snooze feedback.
- Build and deployment configuration (signing, release builds).

## Active Decisions

- The watch app is **headless** — no Activity, only services and receivers.
- Settings sync is **unidirectional** (phone → watch only).
- `AlarmManager.setExactAndAllowWhileIdle()` is used for Doze-mode reliability.
- `goAsync()` + coroutine pattern is used in `VibeReceiver` to allow suspend calls within the broadcast receiver lifecycle.

## Known Issues

- None currently tracked. The project has just reached its initial implementation milestone.
