# VILD – Progress

> Last updated: 2026-04-01T09:04 UTC-6

## Completed

- [x] Project scaffolding with three Gradle modules (`:app`, `:wear`, `:shared`)
- [x] `:shared` — `VibeConstants` with Data Layer path and all DataMap keys
- [x] `:wear` — `VibeSettingsRepository` (SharedPreferences storage)
- [x] `:wear` — `VibeDataListenerService` (Data Layer listener → save → schedule)
- [x] `:wear` — `VibeScheduler` (AlarmManager exact alarms with target-node filtering)
- [x] `:wear` — `VibeReceiver` (vibration + reschedule with WakeLock and goAsync)
- [x] `:wear` — AndroidManifest with permissions and service/receiver declarations
- [x] `:wear` — Minimal `MainActivity` for Android Studio run configuration (calls `finish()`)
- [x] `:app` — `AppSettingsRepository` (DataStore Preferences with Flow)
- [x] `:app` — `WearSyncManager` (Data Layer push with setUrgent)
- [x] `:app` — `MainViewModel` (AndroidViewModel coordinating repo + sync)
- [x] `:app` — `MainActivity` (Jetpack Compose UI with all controls)
- [x] `:app` — UI components: master toggle, node selector, frequency sliders, intensity slider, snooze buttons
- [x] Multi-watch support via target node ID filtering
- [x] README.md documentation
- [x] Memory bank initialization
- [x] New features architecture plan created (`plans/new-features-plan.md`)
- [x] Background redesign: solid black + parallax VILD icon (2026-04-01)
- [x] Advice notes feature: tap any advice banner item to open a personal notes popup, persisted per advice item (2026-04-02)

## In Progress

- Nothing currently in progress — awaiting implementation of planned features.

## Not Yet Started — New Features

- [ ] Add new constants to `VibeConstants` (vibration duration, pattern type, repeat count, vibrate-now path)
- [ ] Update `VibeSettings` data class with new vibration fields + custom snooze durations
- [ ] Update `AppSettingsRepository` with new DataStore keys
- [ ] Update `WearSyncManager` — push new fields + add `sendVibrateNow()` via MessageClient
- [ ] Update watch-side `VibeSettingsRepository` — new field getters/setters
- [ ] Create `VibrationHelper` on watch (extracted vibration logic with pattern support)
- [ ] Update `VibeReceiver` to delegate to `VibrationHelper`
- [ ] Update `VibeDataListenerService` — handle new fields + add `onMessageReceived()` for vibrate-now
- [ ] Update `wear/AndroidManifest.xml` — add MESSAGE_RECEIVED intent filter
- [ ] Update `MainViewModel` — new update methods, vibrateNow, custom snooze, countdown
- [ ] Create `app/ui/VibrationSection.kt` — vibration settings UI + Vibrate Now button
- [ ] Create `app/ui/SnoozeSection.kt` — snooze countdown + custom snooze management
- [ ] Refactor `MainActivity.kt` to use extracted section composables
- [ ] Update README.md with new features documentation

## Not Yet Started — Infrastructure

- [ ] Physical device testing (phone + Wear OS watch pairing)
- [ ] Exact alarm permission handling (Android 12+ runtime prompt)
- [ ] Error handling and edge case coverage
- [ ] Release build configuration (signing, ProGuard)
- [ ] CI/CD pipeline

## Architecture Status

All three modules are implemented and wired together. The data flow is complete:

```
Phone UI → ViewModel → DataStore + DataClient → Data Layer → Watch Listener → SharedPrefs → AlarmManager → BroadcastReceiver → Vibration → Reschedule
```

Planned addition:
```
Phone UI → ViewModel → MessageClient → Watch Listener → VibrationHelper → Immediate Vibration
```

## Known Issues

None currently tracked.

## Bug Fixes Applied

- **2026-03-29T17:08 UTC-6:** Fixed alarm chain breaking after first vibration — `VibeReceiver`, `VibeScheduler`, `VibeDataListenerService`, `BootReceiver` all updated to use `applicationContext`, extended WakeLock, added Play Services timeout, added emergency reschedule.
