# ADR: Wake-detection fallback via daily dream-trigger acknowledgment

**Date:** 2026-09-12
**Status:** Accepted

## Context
The night → day mode transition (wake-up signal) previously depended solely on
Tail's `ACTION_HABIT_INCREMENTED` broadcast received by
`DayModeSwitchReceiver`. When Tail's broadcast was missed (broken sync, app
update, process death), VILD remained stuck in night mode and kept posting
"☾ Dream check" night-vibe notifications during the day.

## Decision
1. Extracted the night → day switch sequence (snapshot night settings, set
   active mode "day", load day settings, `pauseUntilNextNight`, re-arm
   scheduler, cancel any live night notification) into the shared
   `data/DayModeSwitcher.forceDayMode()` object.
2. `DayModeSwitchReceiver` (Tail broadcast) now delegates to it.
3. `RealityCheckActionReceiver` now treats ANY tap on the daily dream-trigger
   notification ("✓ Read" / "✓ Done") as proof of wakefulness and calls the
   same `forceDayMode()` as a fallback. It also ensures today's log exists
   before marking, so the fallback works even if the morning alarm failed.
4. Added `NightVibeNotifier.cancel()` so a stale notification disappears
   immediately upon wake-up instead of waiting out its 60 s auto-dismiss.
5. Removed the entire snooze feature per user request: `ui/SnoozeSection.kt`
   deleted, settings section and imports removed, `snooze()`/`cancelSnooze()`
   /`addCustomSnoozeDuration()`/`removeCustomSnoozeDuration()` and
   `snoozeCountdownText` removed from `MainViewModel`,
   `snoozeUntilTimestamp`/`customSnoozeDurations` removed from
   `NightVibeSettings` (legacy JSON snapshots simply ignore the removed
   optional fields, so serialization stays compatible), scheduler/receiver
   snooze gating dropped.

## Consequences
- Wake detection no longer has a single point of failure: Tail broadcast OR
  dream-trigger tap OR manual toggle all converge on `DayModeSwitcher`.
- The night-vibe chain cannot leak into the day as long as the user
  acknowledges the daily trigger.
- NightVibeSettings JSON: removed fields are optional with defaults, so old
  persisted mode snapshots decode cleanly.
- Snooze UI/state is gone; `nextVibeMs` is simpler (no snooze interpolation).