# VILD – Tech Context

> Last updated: 2026-03-29T15:17 UTC-6

## Build System

- **Gradle** with Kotlin DSL (`.gradle.kts`)
- **Version catalog**: `gradle/libs.versions.toml`
- **AGP**: 8.13.2
- **Kotlin**: 2.0.21
- **Compile SDK**: 36
- **Min SDK**: 26 (both app and wear)
- **Target SDK**: 36
- **JVM target**: 11

## Module Configuration

### `:app` (Phone Companion)

- Plugin: `com.android.application`
- Application ID: `com.example.vild`
- Compose enabled
- Dependencies:
  - `project(":shared")`
  - Jetpack Compose BOM (2024.09.00) + Material 3
  - `androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0`
  - `androidx.datastore:datastore-preferences:1.1.4`
  - `com.google.android.gms:play-services-wearable:18.2.0`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
  - `androidx.activity:activity-compose:1.13.0`

### `:wear` (Wear OS Worker)

- Plugin: `com.android.application`
- Application ID: `com.example.vild.wear`
- Compose enabled (Wear Compose BOM 2024.10.00)
- Dependencies:
  - `project(":shared")`
  - `com.google.android.gms:play-services-wearable:18.2.0`
  - Wear Compose Material + Foundation
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
- Permissions:
  - `VIBRATE`
  - `WAKE_LOCK`
  - `SCHEDULE_EXACT_ALARM`
  - `USE_EXACT_ALARM`
- Manifest declares:
  - `uses-feature android.hardware.type.watch`
  - `MainActivity` with LAUNCHER intent filter (calls `finish()` immediately)
  - `VibeDataListenerService` with `DATA_CHANGED` intent filter for `wear:///vibe_settings`
  - `VibeDataListenerService` with `MESSAGE_RECEIVED` intent filter for `wear:///vibrate_now` (planned)
  - `VibeReceiver` (not exported)

### `:shared` (Library)

- Plugin: `com.android.library`
- Contains only `VibeConstants.kt`

## Key Technical Decisions

| Decision | Rationale |
|----------|-----------|
| `AlarmManager.setExactAndAllowWhileIdle()` | Ensures alarms fire during Doze mode on Wear OS |
| `PutDataRequest.setUrgent()` | Immediate delivery of settings changes rather than batched |
| `goAsync()` + coroutine in receiver | Allows suspend calls within BroadcastReceiver lifecycle |
| SharedPreferences on watch | Synchronous reads needed by AlarmManager/BroadcastReceiver context |
| DataStore on phone | Reactive Flow-based API suits Compose UI observation |
| No watch-side Activity UI | Watch is headless; all config done from phone |
| `WakeLock` with 3s timeout | Prevents device sleeping during vibration + reschedule |
| `MessageClient` for vibrate-now (planned) | Fire-and-forget semantics; no persistent state needed for one-shot commands |
| `VibrationHelper` extraction (planned) | Shared vibration logic between scheduled and immediate vibrations |
| UI decomposition into section files (planned) | Keep files under 500 lines per project guidelines |

## Data Layer Communication

- **Settings Path**: `/vibe_settings`
- **Settings Mechanism**: `DataClient.putDataItem()` (phone) → `WearableListenerService.onDataChanged()` (watch)
- **Settings Delivery**: Automatic to all paired nodes; queued for offline nodes
- **Settings Keys**: Defined in `VibeConstants` — `is_enabled`, `freq_min_minutes`, `freq_max_minutes`, `vibration_intensity`, `snooze_until_timestamp`, `target_node_id`
- **Planned new keys**: `vibration_duration_ms`, `vibration_pattern_type`, `vibration_repeat_count`

### Message Communication (Planned)

- **Vibrate-Now Path**: `/vibrate_now`
- **Mechanism**: `MessageClient.sendMessage()` (phone) → `WearableListenerService.onMessageReceived()` (watch)
- **Delivery**: Requires watch to be currently connected; not queued for offline nodes

## Development Environment

- Android Studio project
- Linux development machine
- No CI/CD configured yet
- No signing configuration for release builds yet
