# ADR-005: Authoritative Tail backfill (0 = clear)

## Context
After initial Tail integration (ADR-004) the user reported a phantom "done" point: during setup the READ slot was briefly mapped to the *Done* habit, and its auto-backfill wrote today's read point into "Reality Check Done". The old backfill only ever sent positive values, so nothing ever corrected Tail — the wrong point was permanent. A latent second bug: legacy day logs (created before `doneCount` existed) have `doneAt` set but `doneCount = 0`, so `filter { it.doneCount > 0 }` silently excluded them from the DONE backfill.

## Decision
1. `MainViewModel.backfillTail()` is now **fully authoritative**: for every day VILD has a log it sends READ = `readAt != null ? 1 : 0` and DONE = `doneAt != null ? maxOf(1, doneCount) : 0`. Tail's `HabitValueSetReceiver` treats 0 as "remove this date's entry", so any wrongly-pushed point self-heals on the next Send. Days without a VILD log are never touched (Tail's own data is preserved).
2. The habit picker is hardened: the dialog title names the slot ("Read"/"Done") plus a one-line description of when it fires, and a habit already mapped to the other slot is disabled with a hint — both slots can never point at the same habit again.
3. Direct adb `am broadcast` cannot clear Tail data: the shell uid does not hold `com.example.tail.permission.TAIL_INTEGRATION`, so the signature-permission guard drops the broadcast (verified on device). Only same-keystore apps (VILD/WAGS) can write.

## Consequences
- Backfill is idempotent AND corrective — re-running Send always converges Tail to VILD's truth for logged days.
- If the user manually adds Tail points on days VILD also logged, a backfill will overwrite/clear them for those days only (hint text in Settings says so).
- Healing a wrong point requires tapping Send (or re-connecting a habit, which auto-backfills); no automatic backfill on app start (deliberate — avoids surprise overwrites).