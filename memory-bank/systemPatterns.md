# VILD – System Patterns

> Last updated: 2026-03-29

## Architecture Overview

```mermaid
graph TD
    subgraph Phone
        UI[MainActivity - Compose UI]
        VM[MainViewModel]
        Repo[AppSettingsRepository - DataStore]
        Sync[WearSyncManager - DataClient]
    end

    subgraph Shared
        Const[VibeConstants]
    end

    subgraph Watch
        Listener[VibeDataListenerService]
        WRepo[VibeSettingsRepository - SharedPrefs]
        Sched[VibeScheduler - AlarmManager]
        Recv[VibeReceiver - BroadcastReceiver]
    end

    UI --> VM
    VM --> Repo
    VM --> Sync
    Sync -->|DataClient.putDataItem| Listener
    Listener --> WRepo
    Listener --> Sched
    Sched -->|AlarmManager| Recv
    Recv -->|vibrate + reschedule| Sched
    Const -.->|used by| Sync
    Const -.->|used by| Listener
    Const -.->|used by| Repo
    Const -.->|used by| WRepo
```

## Key Patterns

### 1. Unidirectional Data Flow (Phone → Watch)

Settings flow in one direction only:
1. User changes a setting in the Compose UI.
2. `MainViewModel.updateAndSync()` saves locally via `AppSettingsRepository` AND pushes to the Data Layer via `WearSyncManager`.
3. The watch's `VibeDataListenerService` receives the update, persists to `VibeSettingsRepository`, and triggers `VibeScheduler`.

### 2. Shared Constants Module

The `:shared` module contains `VibeConstants` — a single Kotlin `object` with all Data Layer paths and DataMap keys. Both `:app` and `:wear` depend on `:shared`, ensuring key strings are never duplicated or mismatched.

### 3. AlarmManager + BroadcastReceiver Chain

The vibration scheduling uses a self-rescheduling pattern:
1. `VibeScheduler.schedule()` sets an exact alarm via `AlarmManager.setExactAndAllowWhileIdle()`.
2. When the alarm fires, `VibeReceiver.onReceive()` vibrates the device and calls `VibeScheduler.schedule()` again to set the next alarm.
3. This creates a continuous chain of random-interval vibrations.

### 4. Target Node Filtering

Before scheduling an alarm, `VibeScheduler.isThisNodeTargeted()` checks:
- If `target_node_id == "all"` → schedule (all watches active).
- Otherwise, fetch the local node ID via `Wearable.getNodeClient().localNode` and compare.
- If no match → cancel any existing alarm.

### 5. goAsync + Coroutine in BroadcastReceiver

`VibeReceiver` uses `goAsync()` to extend the receiver lifecycle, then launches a coroutine to perform the suspend call to `VibeScheduler.schedule()` (which fetches the local node ID). A `WakeLock` ensures the device stays awake during this work.

### 6. ViewModel as Single Coordinator

`MainViewModel` is the sole coordinator between the UI, local persistence, and Data Layer sync. Every `update*()` method follows the same pattern:
1. Update the in-memory `StateFlow`.
2. Launch a coroutine to save locally + push to Data Layer.

### 7. DataStore vs SharedPreferences

- **Phone side**: Uses Jetpack DataStore Preferences (reactive `Flow`-based API).
- **Watch side**: Uses plain `SharedPreferences` (simpler, synchronous reads needed by `VibeReceiver`).

## File Organization

```
VILD/
├── app/                          # Phone companion (com.example.vild)
│   └── src/main/java/.../
│       ├── MainActivity.kt       # Compose UI entry point
│       ├── MainViewModel.kt      # UI state + coordination
│       └── data/
│           ├── AppSettingsRepository.kt  # DataStore persistence
│           └── WearSyncManager.kt        # Data Layer push
├── wear/                         # Wear OS worker (com.example.vild.wear)
│   └── src/main/java/.../wear/
│       ├── VibeDataListenerService.kt    # Data Layer listener
│       ├── VibeSettingsRepository.kt     # SharedPreferences storage
│       ├── VibeScheduler.kt              # AlarmManager scheduling
│       └── VibeReceiver.kt              # Vibration + reschedule
└── shared/                       # Shared library (com.example.vild.shared)
    └── src/main/java/.../shared/
        └── VibeConstants.kt              # Paths and keys
```
