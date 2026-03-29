# VILD – Vibration Interval Learning Device

> Last updated: 2026-03-29T21:00 UTC

A two-module Android project that turns a paired TicWatch (Wear OS) into a mindfulness vibration reminder, controlled from a companion phone app.

---

## Project Structure

```
VILD/
├── app/          – Phone companion app (remote control UI)
├── wear/         – Wear OS app (vibration scheduler)
└── shared/       – Kotlin library shared by both modules
```

---

## Modules

### `:shared`
Single source of truth for Wearable Data Layer constants.

| File | Purpose |
|------|---------|
| `VibeConstants.kt` | Data Layer path (`/vibe_settings`) and all DataMap keys |

**Keys**

| Constant | Type | Description |
|----------|------|-------------|
| `KEY_IS_ENABLED` | Boolean | Master on/off switch |
| `KEY_FREQ_MIN_MINUTES` | Int | Minimum reminder interval (minutes) |
| `KEY_FREQ_MAX_MINUTES` | Int | Maximum reminder interval (minutes) |
| `KEY_VIBRATION_INTENSITY` | Int | Motor intensity 1–255 |
| `KEY_SNOOZE_UNTIL_TIMESTAMP` | Long | Epoch-ms until which reminders are paused |
| `KEY_TARGET_NODE_ID` | String | Node ID of the active watch; `"all"` = every node |

---

### `:wear`
Wear OS application that receives settings from the phone and schedules vibration alarms.

| File | Purpose |
|------|---------|
| `VibeSettingsRepository.kt` | SharedPreferences storage for settings on the watch |
| `VibeDataListenerService.kt` | `WearableListenerService` – receives Data Layer updates, saves settings, triggers scheduler |
| `VibeScheduler.kt` | Schedules/cancels `AlarmManager` alarms; checks `target_node_id` before scheduling |
| `VibeReceiver.kt` | `BroadcastReceiver` – fires vibration and reschedules next alarm |

**Active-watch logic:** `VibeScheduler.schedule()` fetches the local Wear OS node ID via `Wearable.getNodeClient` and compares it to `KEY_TARGET_NODE_ID`. If they don't match (and the target is not `"all"`), the alarm is cancelled rather than scheduled.

---

### `:app`
Phone companion app built with Jetpack Compose.

| File | Purpose |
|------|---------|
| `data/AppSettingsRepository.kt` | DataStore Preferences – persists settings locally on the phone |
| `data/WearSyncManager.kt` | Wearable Data Layer client – pushes settings to all paired nodes |
| `MainViewModel.kt` | AndroidViewModel – holds UI state, coordinates repo + sync |
| `MainActivity.kt` | Compose UI entry point |

#### UI Screens / Components

| Component | Description |
|-----------|-------------|
| Master Toggle | Switch to enable/disable vibration reminders |
| Node Selector | Dropdown listing connected Wear OS nodes; select the "active watch" or "All watches" |
| Frequency Sliders | Min/max interval sliders (1–120 min); min is clamped ≤ max |
| Intensity Slider | Vibration motor intensity 1–255 |
| Snooze Buttons | 15 min / 30 min / 1 hr quick-snooze buttons |

#### Multi-watch support
`WearSyncManager.pushSettings()` calls `DataClient.putDataItem()` which the Wearable Data Layer automatically delivers to **all** currently connected nodes and queues for nodes that are offline. When a disconnected watch reconnects, it receives the latest settings automatically.

To designate only one watch as the active vibrator, select it in the **Node Selector** dropdown. The phone pushes `KEY_TARGET_NODE_ID` = `<nodeId>` and the Wear app on each watch checks whether its own node ID matches before scheduling alarms.

---

## Data Flow

```
Phone UI change
    │
    ▼
MainViewModel.update*()
    │
    ├─► AppSettingsRepository.save()   (local DataStore)
    │
    └─► WearSyncManager.pushSettings() (DataClient.putDataItem)
              │
              ▼
        Wearable Data Layer
              │
    ┌─────────┴──────────┐
    ▼                    ▼
Watch A                Watch B
VibeDataListenerService  VibeDataListenerService
    │                    │
    ▼                    ▼
VibeSettingsRepository  VibeSettingsRepository
    │                    │
    ▼                    ▼
VibeScheduler           VibeScheduler
(checks target_node_id) (checks target_node_id)
```

---

## Dependencies

| Library | Used in |
|---------|---------|
| Jetpack Compose + Material 3 | `:app` |
| `lifecycle-viewmodel-compose` | `:app` |
| `datastore-preferences` | `:app` |
| `play-services-wearable` | `:app`, `:wear` |
| `kotlinx-coroutines-play-services` | `:app`, `:wear` |
| `wear-compose-material` 1.3.0 | `:wear` |
| `wear-compose-foundation` 1.3.0 | `:wear` |

---

## Changelog

### 2026-03-29T21:00 UTC
- Added `alias(libs.plugins.android.library) apply false` to root [`build.gradle.kts`](build.gradle.kts) — fixes "plugin already on classpath with unknown version" Gradle error for the `:shared` module.
- Replaced non-existent `wear-compose-bom:2024.10.00` with explicit `wear-compose-material:1.3.0` and `wear-compose-foundation:1.3.0` versions in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`wear/build.gradle.kts`](wear/build.gradle.kts).
- Added `kotlinx-coroutines-play-services:1.9.0` dependency to both `:app` and `:wear` modules to resolve `kotlinx.coroutines.tasks.await` unresolved reference.
- Added minimal [`wear/src/main/java/com/example/vild/wear/MainActivity.kt`](wear/src/main/java/com/example/vild/wear/MainActivity.kt) with `LAUNCHER` intent filter so the `:wear` module appears in Android Studio run configurations.
- Removed missing `@mipmap/ic_launcher` reference from [`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml).
- Build verified: `./gradlew assembleDebug` → **BUILD SUCCESSFUL** in 16s.
