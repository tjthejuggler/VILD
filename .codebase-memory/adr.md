# ADR: Implied wake-up detection — no wake-up-time setting (2026-09-13)

## Context
Night vibes kept firing after sunrise (2026-09-13 incident): `DayModeSwitcher.forceDayMode` early-returned when the app was already in day mode, skipping the bedtime-anchor clear — the vibe chain stayed armed all morning. The codebase also had a wake-up-time slider and a default-off "auto switch day on habit" toggle, and in-app reality-check taps never signalled wake at all.

## Decision
1. **Wake-up is implied, never configured.** There is no wake-up-time setting. Any of these signals ends the night: (a) first manual habit increment in Tail (`TailHabitSyncReceiver`), (b) "I read it" / "I did it" tap on the notification (`RealityCheckActionReceiver`), (c) the same taps inside the app (`MainViewModel.markToday`), (d) the "Good morning" button on the new morning prompt (`MorningActionReceiver`), (e) the manual Day/Night toggle.
2. **`forceDayMode` is the single wake-up sequence** and ALWAYS clears the bedtime anchor + cancels vibe/morning notifications, regardless of active mode; the night→day settings swap happens only when actually in night mode.
3. **Morning prompt** (`MorningPromptNotifier`, ID 3003, channel `vild_morning_prompt`) is armed daily at the night-end time by `NightVibeScheduler`; `MorningPromptReceiver` gates delivery on "night vibes enabled AND a live bedtime anchor exists" so it never nags an already-awake user.
4. **No opt-in toggle.** The `autoSwitchDayOnHabit` setting, its flow, and `DayModeSwitchReceiver` were removed; wake-on-Tail-increment is now unconditional (echo-suppressed via EXTRA_SOURCE).

## Consequences
- A stale anchor can no longer outlive a wake-up signal — the morning-vibes bug class is eliminated structurally.
- The night-end slider remains as the vibe chain's wall-clock backstop; it now also schedules the morning prompt.
- Any new wake-up source must call `DayModeSwitcher.forceDayMode` to stay consistent.