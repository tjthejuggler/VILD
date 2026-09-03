# ADR: Adaptive goal-based nag scheduling

Date: 2026-09-03

## Context
The nag cycle previously re-posted the reality-check notification every fixed 30 minutes until the first READ + DONE of the day, ignoring how much practice the user actually completed.

## Decision
- Daily goals live on RealityCheckDayLog: DAILY_READ_GOAL = 5, DAILY_DONE_GOAL = 2, with derived `unitsRemaining` and `goalsMet`.
- NagScheduler.nextIntervalMs(log) spreads remaining rounds evenly over the time left until 22:00 local: `timeLeftMs / unitsRemaining`, clamped to [20 min, 3 h]. Returns null (nag cancels) when goals are met or the day is over.
- All schedulers/receivers (NagReceiver, DailyTriggerReceiver, RealityCheckActionReceiver, TailHabitSyncReceiver, BootReceiver, MainViewModel) stop nagging on `goalsMet` instead of the old `isComplete`.

## Consequences
- Fewer completed rounds → more frequent notifications; progress → sparser notifications; goals met → silence until the next 8 AM trigger.
- Known issue: `app` and `wear` modules share applicationId `com.example.vild`, so a root `./gradlew installDebug` overwrites the phone app with the wear APK on the same device. Deploy with `:app:installDebug` / `:wear:installDebug` per target until the wear applicationId is made distinct.