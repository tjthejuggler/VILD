# VILD – Product Context

> Last updated: 2026-03-29T15:47 UTC-6

## Why This Project Exists

VILD stands for **Vibration Interval Learning Device**. It is designed to support mindfulness and lucid-dreaming practice by delivering random-interval vibration cues on a wrist-worn Wear OS device. The randomness prevents habituation — the user cannot predict when the next vibration will occur, which keeps the cue effective as a "reality check" prompt.

## How It Works (User Perspective)

1. **Install** the phone companion app and the Wear OS app on a paired watch.
2. **Open the phone app** and configure:
   - Toggle reminders on/off.
   - Set the min/max interval range (e.g. 5–30 minutes).
   - Adjust vibration intensity, duration, pattern, and repeat count.
   - Select which watch should vibrate (or all watches).
3. **Put the phone away** — the watch now operates autonomously, vibrating at random intervals within the configured range.
4. **Snooze** from the phone app if needed (default: 15 min / 30 min / 1 hr, plus user-created custom durations). Cancel an active snooze at any time.
5. **Test vibration** with the "Vibrate Now" button to verify settings feel right.
6. **Save presets** — name and save the current configuration, load it later by name, or delete it.
7. **Day/Night mode** — toggle between two independent sets of settings. Load different presets into each mode.

## User Experience Design

- **Phone app** is the primary UI surface. It uses Material 3 / Jetpack Compose with a single scrollable screen containing:
  - **Day/Night mode toggle** (at the very top)
  - Master toggle switch
  - Sync status indicator (last sync time + success/failure)
  - Node selector dropdown (with refresh button)
  - Min/max frequency sliders
  - Vibration settings section: intensity slider (1–255), duration slider (100–4000ms), pattern selector, repeat count, and "Vibrate Now" button
  - Presets section: save current settings as named preset, load/delete saved presets
  - Snooze section: live countdown when active with cancel button, default snooze buttons, custom snooze buttons with add/remove
- **Watch app** has a minimal Compose UI showing "VILD is active" with a Test Vibration button. The real work is done by a `WearableListenerService`, `AlarmManager`, and `BroadcastReceiver`.

## Multi-Watch Support

The system supports multiple paired Wear OS watches. The phone pushes settings to the Data Layer, which delivers to all connected nodes. Each watch checks whether its own node ID matches the `target_node_id` setting before scheduling alarms. This allows the user to:
- Target a single specific watch, or
- Target all watches simultaneously (using the `"all"` sentinel value).

## Sync Mechanism

Settings sync uses the **Android Wear Data Layer API** (`DataClient.putDataItem` with `setUrgent()`). This is automatic — no manual sync button is needed. The Data Layer:
- Delivers immediately to all connected watches.
- Queues updates for offline watches and delivers when they reconnect.
- The watch persists settings locally in SharedPreferences, so it operates autonomously even when disconnected.

A sync status indicator on the phone shows whether the last push succeeded and when.

## Hardware Limits

- **Vibration intensity max: 255** — This is the Android `VibrationEffect` API hardware limit for amplitude. It cannot be exceeded.
- **Vibration duration max: 4000ms** — Configurable up to 4 seconds per pulse.

## Planned Features (2026-03-29)

1. ~~Immediate Vibrate Button~~ ✅ Implemented
2. ~~Vibration Customization~~ ✅ Implemented
3. ~~Custom Snooze Buttons~~ ✅ Implemented
4. ~~Snooze Status Display~~ ✅ Implemented
5. **Sync Status Indicator** — Show last sync time and success/failure in the phone UI.
6. **Duration Slider Max → 4000ms** — Increase from current 2000ms limit.
7. **Named Presets** — Save, load by name, and delete presets for all settings.
8. **Snooze Cancel** — Easy button to cancel an active snooze.
9. **Day/Night Mode** — Toggle at the top; each mode has independent settings; presets can be loaded into either mode.
