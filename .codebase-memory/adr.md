# ADR: "I read it" is repeatable, symmetric with "I did it"

**Status:** Accepted (2026-08-23)

## Context
The daily reality check has two confirmations: READ and DONE. DONE was designed as a repeatable action (first tap sets `doneAt`, every tap bumps `doneCount` and sends a Tail habit increment). READ was once-per-day: `markReadToday()` returned null after the first tap, the in-app button disabled itself, the notification dropped its "✓ I read it" action, and the Tail reverse-sync applied READ increments only once.

The user wants both buttons to behave identically everywhere: repeatable, in-app, in the notification, and in the Tail integration (both directions).

## Decision
READ becomes an exact structural mirror of DONE:

1. `RealityCheckDayLog` gains `readCount: Int` (default 0), analogous to `doneCount`. `readAt` keeps its "first tap" semantics (`readAt ?: now`) exactly like `doneAt`.
2. `RealityCheckStatsRepository.markReadToday()` mirrors `markDoneToday()`: always applies `readAt = readAt ?: now, readCount += 1` and returns the updated log (null only when no log exists for today).
3. `RealityCheckActionReceiver` (notification actions) and `MainViewModel.markRead` (in-app button): every tap sends a Tail READ habit increment — the old null-based dedupe is gone.
4. `NotificationHelper`: the "✓ I read it" action is always present (previously hidden after first read); status line shows `×N` read rounds like done rounds.
5. `RealityCheckCard`: read button is always enabled; label mirrors done ("✓ Read ×N — again?").
6. `TailHabitSyncReceiver` (Tail → VILD): READ habit increments apply per-amount via `repeat(amount)`, same as DONE.
7. `MainViewModel.backfillTail()`: READ backfill sends `maxOf(1, readCount)` per day (legacy logs predate `readCount`, so `readAt != null && readCount == 0` counts as 1 — same rule DONE already used).

## Consequences
- Stats/streaks are day-level (`readAt != null`) and are unchanged by extra taps.
- `readCount` is backward-compatible: kotlinx.serialization defaults it to 0 for existing JSON logs.
- Echo-suppression loop safety between VILD and Tail is untouched (EXTRA_SOURCE mechanism).
