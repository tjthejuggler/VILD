# ADR: Night-vibe log pollution cleanup & history surface split (2026-09-10)

## Context
The 2026-09-08/09 scheduler bug (fixed 2026-09-10T07:03) fired night vibes all day, flooding the `night_vibe_log` DataStore with daytime pulses. The Settings "Previous nights" section showed up to 3 nights inline, and the marker chips ("In dream" / "Woke me") were too wide for a pulse row.

## Decision
1. **One-time data purge**: `NightVibeLogRepository.purgePollutedDaysOnce()` deletes every entry whose timestamp falls on local dates 2026-09-08 or 2026-09-09, guarded by a `pollution_purge_done_v1` boolean DataStore key so it never runs twice. Invoked once from `MainViewModel.init`.
2. **History surface split**: Settings screen shows only the **last 2 nights**; the complete history stays in the DataStore (14-day retention) and is reachable via the `NightVibeChartActivity` page, relabelled "Full history & chart". No data is ever deleted by the UI split — display-only truncation.
3. **Chip labels shortened** for comfortable fit in both surfaces: "In dream" → "Dream", "Woke me" → "Woke". `NightVibeMark` enum values and persisted flags are unchanged (labels are UI-only).

## Consequences
- Users start with a clean history after the bug; older legit history (pre-Sep-8) is preserved.
- Full historical data remains queryable on the chart page.
- The purge key remains in DataStore harmlessly; future mass-cleanup needs a new flag version (`_v2`).