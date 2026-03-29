# VILD – Vibration Interval Learning Device

> Last updated: 2026-03-29T22:00 UTC

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
| `data/AppSettingsRepository.kt` | DataStore Preferences – persists settings locally on the phone; stores Day/Night mode snapshots as JSON |
| `data/Preset.kt` | `@Serializable` data class capturing a named snapshot of vibration/scheduling settings |
| `data/WearSyncManager.kt` | Wearable Data Layer client – pushes settings to all paired nodes |
| `MainViewModel.kt` | AndroidViewModel – holds UI state, coordinates repo + sync; manages Day/Night mode switching |
| `MainActivity.kt` | Compose UI entry point |
| `ui/PresetSection.kt` | Preset save/load/delete UI component |

#### UI Screens / Components

| Component | Description |
|-----------|-------------|
| Day/Night Toggle | Segmented ☀️ Day / 🌙 Night button pair at the top of the screen; each mode stores independent settings |
| Master Toggle | Switch to enable/disable vibration reminders |
| Node Selector | Dropdown listing connected Wear OS nodes; select the "active watch" or "All watches" |
| Frequency Sliders | Min/max interval sliders (1–120 min); min is clamped ≤ max |
| Intensity Slider | Vibration motor intensity 1–255 |
| Snooze Buttons | 15 min / 30 min / 1 hr quick-snooze buttons |
| Preset Section | Save current settings as a named preset; load or delete saved presets (applies to the currently active Day/Night mode) |

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
| `kotlinx-serialization-json` 1.7.3 | `:app` |
| `wear-compose-material` 1.3.0 | `:wear` |
| `wear-compose-foundation` 1.3.0 | `:wear` |

---

## Changelog

### 2026-03-29T22:00 UTC
- **Phase 4 – Day/Night Mode:**
  - [`app/src/main/java/com/example/vild/data/AppSettingsRepository.kt`](app/src/main/java/com/example/vild/data/AppSettingsRepository.kt): Added `@Serializable` to `VibeSettings` (required for JSON mode snapshots). Added `keyActiveMode`, `keyDaySettings`, `keyNightSettings` DataStore keys. Added `activeModeFlow: Flow<String>` (emits `"day"` or `"night"`). Added `saveModeSettings(mode, settings)`, `loadModeSettings(mode): VibeSettings`, and `setActiveMode(mode)`.
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `_activeMode: MutableStateFlow<String>` and exposed `activeMode: StateFlow<String>`. Added `toggleMode()` — saves current settings under the outgoing mode, switches `activeMode` in DataStore, loads the incoming mode's settings, and syncs to the watch. Updated `loadPreset()` to also call `repo.saveModeSettings(activeMode, newSettings)` so the preset is persisted under the active mode's key.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added `DayNightToggle` composable — a two-button segmented row (☀️ Day / 🌙 Night) placed directly below the `SyncStatusBar`. The active mode button is filled (Night uses `secondary` color); the inactive one is outlined. Clicking the inactive button calls `vm.toggleMode()`.

### 2026-03-29T21:56 UTC
- **Phase 3 – Named Presets:**
  - **New:** [`app/src/main/java/com/example/vild/data/Preset.kt`](app/src/main/java/com/example/vild/data/Preset.kt): `@Serializable` data class capturing a named snapshot of vibration/scheduling settings (`name`, `isEnabled`, `freqMinMinutes`, `freqMaxMinutes`, `vibrationIntensity`, `vibrationDurationMs`, `vibrationPatternType`, `vibrationRepeatCount`). Excludes transient fields (`snoozeUntilTimestamp`, `targetNodeId`, `customSnoozeDurations`).
  - [`app/src/main/java/com/example/vild/data/AppSettingsRepository.kt`](app/src/main/java/com/example/vild/data/AppSettingsRepository.kt): Added `keyPresets = stringPreferencesKey("presets_json")`. Added `presetsFlow: Flow<List<Preset>>` (deserializes JSON from DataStore). Added `savePreset(preset)` (upserts by name) and `deletePreset(name)` (removes by name).
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `presets: StateFlow<List<Preset>>` collected from `repo.presetsFlow`. Added `saveCurrentAsPreset(name)`, `loadPreset(preset)` (applies preset fields and syncs to watch), and `deletePreset(name)`.
  - **New:** [`app/src/main/java/com/example/vild/ui/PresetSection.kt`](app/src/main/java/com/example/vild/ui/PresetSection.kt): Composable section with a "Save current settings as preset" button (opens name dialog), a list of saved presets each with Load/Delete actions, and a delete confirmation dialog. Shows "No saved presets" placeholder when the list is empty.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added `PresetSection` between the Vibration and Snooze sections.

### 2026-03-29T21:54 UTC
- **Phase 2 – Sync Status Indicator:**
  - [`app/src/main/java/com/example/vild/data/WearSyncManager.kt`](app/src/main/java/com/example/vild/data/WearSyncManager.kt): `pushSettings()` now returns `Boolean` — `true` on success, `false` if an exception is thrown.
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `SyncStatus` data class (`lastSyncTimestamp: Long`, `lastSyncSuccess: Boolean`). Added `_syncStatus: MutableStateFlow<SyncStatus>` and exposed `syncStatus: StateFlow<SyncStatus>`. `updateAndSync()` now captures the `Boolean` result from `pushSettings()` and updates `_syncStatus` with the current timestamp and success flag.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added `SyncStatusBar` composable — a slim `Surface` banner at the top of the content area. Hidden until the first sync attempt. Shows green `primaryContainer` + "✓ Synced Xs ago" on success; red `errorContainer` + "✗ Sync failed" on failure. `VildApp` collects `vm.syncStatus` and passes it to `SyncStatusBar`.

### 2026-03-29T21:51 UTC
- **Phase 5 (prereq):** Added `kotlinx-serialization-json:1.7.3` dependency to [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`app/build.gradle.kts`](app/build.gradle.kts). Also added the `kotlin-serialization` Gradle plugin to both files (required for `@Serializable` annotation processing in future phases).
- **Phase 1 – Duration slider:** Increased vibration duration slider max from 2000ms → 4000ms in [`app/src/main/java/com/example/vild/ui/VibrationSection.kt`](app/src/main/java/com/example/vild/ui/VibrationSection.kt). Updated `steps` from 37 → 77 to maintain 50ms granularity.
- **Phase 1 – Cancel snooze:** Added `cancelSnooze()` to [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt) — resets `snoozeUntilTimestamp` to 0 and syncs to watch. Added a "Cancel snooze" `TextButton` in [`app/src/main/java/com/example/vild/ui/SnoozeSection.kt`](app/src/main/java/com/example/vild/ui/SnoozeSection.kt) that appears inline with the countdown text only when a snooze is active.

### 2026-03-29T21:31 UTC
- Replaced `finish()`-only [`wear/src/main/java/com/example/vild/wear/MainActivity.kt`](wear/src/main/java/com/example/vild/wear/MainActivity.kt) with a persistent Wear OS Compose UI (`ComponentActivity` + `setContent`). The screen displays "VILD is active. Configure settings from your phone." — keeping the activity alive so the OS does not kill `VibeDataListenerService`.
- Added a **Test Vibration** button to the watch UI that calls `VibrationHelper.vibrate(context)` directly, allowing on-device verification of the vibrator.
- Added `androidx-activity-compose` dependency to [`wear/build.gradle.kts`](wear/build.gradle.kts) (required for `setContent` in `ComponentActivity`).
- Confirmed `android.permission.VIBRATE` is present in [`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml) ✅
- Confirmed `VibeDataListenerService` is registered with both `DATA_CHANGED` and `MESSAGE_RECEIVED` intent filters in the wear manifest ✅

### 2026-03-29T21:00 UTC
- Added `alias(libs.plugins.android.library) apply false` to root [`build.gradle.kts`](build.gradle.kts) — fixes "plugin already on classpath with unknown version" Gradle error for the `:shared` module.
- Replaced non-existent `wear-compose-bom:2024.10.00` with explicit `wear-compose-material:1.3.0` and `wear-compose-foundation:1.3.0` versions in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`wear/build.gradle.kts`](wear/build.gradle.kts).
- Added `kotlinx-coroutines-play-services:1.9.0` dependency to both `:app` and `:wear` modules to resolve `kotlinx.coroutines.tasks.await` unresolved reference.
- Added minimal [`wear/src/main/java/com/example/vild/wear/MainActivity.kt`](wear/src/main/java/com/example/vild/wear/MainActivity.kt) with `LAUNCHER` intent filter so the `:wear` module appears in Android Studio run configurations.
- Removed missing `@mipmap/ic_launcher` reference from [`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml).
- Build verified: `./gradlew assembleDebug` → **BUILD SUCCESSFUL** in 16s.
