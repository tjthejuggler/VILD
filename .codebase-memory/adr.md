# ADR: Bedtime-anchored scheduling + adaptive REM interval learning (2026-09-10)

## Context
Night vibes fired on fixed clock times (nightStartMinutes + fixed gap), regardless of when the user actually went to sleep. Users also had to manually tune the cycle length; the ideal interval (the one producing in-dream detections) was never learned from feedback.

## Decision
1. **Goodnight anchor model**: Each evening at a configurable prompt time (`bedtimePromptMinutes`, default 22:30), a "Going to sleep?" notification offers a Goodnight action. Tapping it stamps `bedtimeAnchorMs`. The quiet gap and every REM pulse are measured from that anchor — scheduling is fully relative to real bedtime. `nightEndMinutes` remains a wall-clock cap so late bedtimes can never leak vibes into the day. `nightStartMinutes` is retained only for legacy day/night snapshot deserialization.
2. **Two-alarm architecture**: independent `setAlarmClock` PendingIntents for the prompt (daily, always armed while enabled) and the vibe chain (armed only when a fresh <20h anchor exists). Wake-up (Day-mode switch) clears the anchor instead of the legacy snooze-based pause.
3. **Batched adaptive learning** (`SleepLearning`): only deliberately annotated pulses count (`NightVibeEntry.annotated` flag — the Unnoticed default does NOT count); ≥3 annotations required per batch; recency-weighted (0.5^(age/6)); Dream detections dominate → interval moves toward the spacing that produced the dream hit; Woke dominates → interval widens; Unnoticed is neutral. Movement capped at ±10 min/batch, clamped 60–120 min. Learning runs after each vibe fire, toggleable via `adaptiveInterval`.

## Consequences
- Users who go to bed at different times get correctly-timed vibes every night.
- The interval converges toward the user's real dream-detection window as feedback accumulates.
- Stale anchors (>20h) auto-invalidate, so forgotten phones don't fire a dead chain.
- Feedback "spent" on a batch still counts in later evaluations (no destructive consumption); the batch gate is only the ≥3-annotation threshold.