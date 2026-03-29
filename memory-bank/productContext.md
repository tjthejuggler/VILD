# VILD – Product Context

> Last updated: 2026-03-29

## Why This Project Exists

VILD stands for **Vibration Interval Learning Device**. It is designed to support mindfulness and lucid-dreaming practice by delivering random-interval vibration cues on a wrist-worn Wear OS device. The randomness prevents habituation — the user cannot predict when the next vibration will occur, which keeps the cue effective as a "reality check" prompt.

## How It Works (User Perspective)

1. **Install** the phone companion app and the Wear OS app on a paired watch.
2. **Open the phone app** and configure:
   - Toggle reminders on/off.
   - Set the min/max interval range (e.g. 5–30 minutes).
   - Adjust vibration intensity.
   - Select which watch should vibrate (or all watches).
3. **Put the phone away** — the watch now operates autonomously, vibrating at random intervals within the configured range.
4. **Snooze** from the phone app if needed (15 min / 30 min / 1 hr).

## User Experience Design

- **Phone app** is the only UI surface. It uses Material 3 / Jetpack Compose with a single scrollable screen containing:
  - Master toggle switch
  - Node selector dropdown (with refresh button)
  - Min/max frequency sliders
  - Intensity slider
  - Snooze buttons with active-snooze indicator
- **Watch app** is headless — no watch-side UI. It runs entirely via a `WearableListenerService`, `AlarmManager`, and `BroadcastReceiver`.

## Multi-Watch Support

The system supports multiple paired Wear OS watches. The phone pushes settings to the Data Layer, which delivers to all connected nodes. Each watch checks whether its own node ID matches the `target_node_id` setting before scheduling alarms. This allows the user to:
- Target a single specific watch, or
- Target all watches simultaneously (using the `"all"` sentinel value).
