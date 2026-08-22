# VILD – Vibration Interval Learning Device

> Last updated: 2026-08-22T12:06 UTC

A two-module Android project that turns a paired TicWatch (Wear OS) into a mindfulness vibration reminder, controlled from a companion phone app.

**The daily reality check is now the heart of the app:** every morning one of your triggers is chosen, shown immediately on the dream-like main screen, and the app *insists* — with an un-dismissable, self-re-posting notification — until you confirm you have both **read** and **done** the check. Streaks and stats are tracked in the Dream Stats screen. Watch vibration remains as a secondary reminder layer, configured on the Settings screen.

---

## Project Structure

```
VILD/
├── app/          – Phone companion app (remote control UI)
├── wear/         – Wear OS app (vibration scheduler)
└── shared/       – Kotlin library shared by both modules
```

---

## Modules

### `:shared`
Single source of truth for Wearable Data Layer constants.

| File | Purpose |
|------|---------|
| `VibeConstants.kt` | Data Layer path (`/vibe_settings`) and all DataMap keys |

**Keys**

| Constant | Type | Description |
|----------|------|-------------|
| `KEY_IS_ENABLED` | Boolean | Master on/off switch |
| `KEY_FREQ_MIN_MINUTES` | Int | Minimum reminder interval (minutes) |
| `KEY_FREQ_MAX_MINUTES` | Int | Maximum reminder interval (minutes) |
| `KEY_VIBRATION_INTENSITY` | Int | Motor intensity 1–255 |
| `KEY_SNOOZE_UNTIL_TIMESTAMP` | Long | Epoch-ms until which reminders are paused |
| `KEY_TARGET_NODE_ID` | String | Node ID of the active watch; `"all"` = every node |

---

### `:wear`
Wear OS application that receives settings from the phone and schedules vibration alarms.

| File | Purpose |
|------|---------|
| `VibeSettingsRepository.kt` | SharedPreferences storage for settings on the watch |
| `VibeDataListenerService.kt` | `WearableListenerService` – receives Data Layer updates, saves settings, triggers scheduler |
| `VibeScheduler.kt` | Schedules/cancels `AlarmManager` alarms via `setAlarmClock()` (Doze-exempt); checks `target_node_id` before scheduling |
| `VibeReceiver.kt` | `BroadcastReceiver` – fires vibration and reschedules next alarm |
| `BootReceiver.kt` | `BroadcastReceiver` – listens for `BOOT_COMPLETED` and reschedules the alarm after reboot |
| `MainActivity.kt` | Wear OS Compose UI; calls `VibeScheduler.schedule()` on every launch to recover from process death |

**Active-watch logic:** `VibeScheduler.schedule()` fetches the local Wear OS node ID via `Wearable.getNodeClient` and compares it to `KEY_TARGET_NODE_ID`. If they don't match (and the target is not `"all"`), the alarm is cancelled rather than scheduled.

**Alarm strategy:** Uses `AlarmManager.setAlarmClock()` instead of `setExactAndAllowWhileIdle()`. The latter has a system-enforced minimum interval of ~10 minutes on API 31+, making it unsuitable for short reminder intervals. `setAlarmClock()` is exempt from all Doze rate-limiting and always fires at the exact requested time. The trade-off is a small alarm icon in the watch status bar.

---

### `:app`
Phone companion app built with Jetpack Compose.

| File | Purpose |
|------|---------|
| `data/AdviceItem.kt` | `@Serializable` data class for a user-entered advice item (id, section, text, createdAt) |
| `data/AdviceRepository.kt` | DataStore-backed repository for CRUD operations on advice items (stored as JSON) |
| `data/AppSettingsRepository.kt` | DataStore Preferences – persists settings locally on the phone; stores Day/Night mode snapshots as JSON |
| `data/Preset.kt` | `@Serializable` data class capturing a named snapshot of vibration/scheduling settings |
| `data/RealityCheckDayLog.kt` | `@Serializable` per-day record: chosen trigger, `readAt`, `doneAt` |
| `data/RealityCheckStats.kt` | Pure stats computation — current/best read & done streaks, totals, per-trigger leaderboard |
| `data/RealityCheckStatsRepository.kt` | DataStore-backed repository for day logs (`ensureTodayLog`, `markReadToday`, `markDoneToday`) |
| `data/RealityCheckTrigger.kt` | `@Serializable` user-entered reality check trigger |
| `data/RealityCheckRepository.kt` | DataStore-backed CRUD for triggers |
| `data/TechniqueItem.kt` | `@Serializable` reality check technique (seeded classic or user-added) |
| `data/TechniqueRepository.kt` | DataStore-backed CRUD for techniques + one-time seeding of 20 classic methods |
| `data/DailyTriggerScheduler.kt` | Schedules the daily 8 AM alarm (`setAlarmClock`, Doze-exempt) |
| `data/NagScheduler.kt` | Arms/disarms the 30-min self-rescheduling nag alarm |
| `data/NotificationHelper.kt` | Builds the ongoing reality check notification with "✓ Read" / "✓ Done" actions |
| `data/WearSyncManager.kt` | Wearable Data Layer client – pushes settings to all paired nodes |
| `ipc/DailyTriggerReceiver.kt` | 8 AM receiver — ensures today's log, posts notification, arms nag |
| `ipc/NagReceiver.kt` | Re-posts the notification every 30 min until the check is confirmed |
| `ipc/RealityCheckActionReceiver.kt` | Handles notification action buttons (mark read / mark done) |
| `ipc/BootReceiver.kt` | Re-arms alarms after reboot |
| `MainViewModel.kt` | AndroidViewModel – UI state, day-log confirmations, streaks, watch sync, Day/Night mode, advice |
| `MainActivity.kt` | Compose entry point — dream main screen (reality check first), stats & settings screens |
| `ui/dream/AccelerometerEffect.kt` | `rememberTiltState()` — low-pass-filtered accelerometer tilt for parallax |
| `ui/dream/DreamBackground.kt` | Living background: void gradient, drifting nebula orbs, twinkling parallax starfield, breathing glow |
| `ui/dream/GlassCard.kt` | Translucent glass card with gradient border, used across all screens |
| `ui/realitycheck/RealityCheckCard.kt` | The main card: today's trigger + "I read it" / "I did it" confirmations + streaks |
| `ui/technique/TechniqueBanner.kt` | Swipeable glass banner showing a random reality check technique |
| `ui/technique/TechniqueDialog.kt` | Manage techniques — add, edit, delete (✦ marks the classics) |
| `ui/stats/StatsScreen.kt` | Dream Stats — streaks, totals, trigger leaderboard, 14-day timeline |
| `ui/advice/AdviceSection.kt` | String constants for the two advice sections ("day" / "night") |
| `ui/advice/AdviceBanner.kt` | Swipeable banner composable showing random advice; swipe left = next, swipe right = previous |
| `ui/advice/AdviceDialog.kt` | Full-screen dialog for adding, editing, and deleting advice items |
| `ui/settings/SettingsScreen.kt` | Secondary screen: vibration & watch tuning, presets, snooze, advice, triggers |
| `ui/PresetSection.kt` | Preset save/load/delete UI component |

#### UI Screens / Components

| Component | Description |
|-----------|-------------|
| Dream Background | Deep void → indigo gradient with three drifting nebula orbs, a 70-star twinkling field and a breathing central glow — all subtly parallaxed by the accelerometer |
| Reality Check Card | Today's trigger in large serif italic, with "I read it" / "I did it" pill buttons and live streaks; glows softly while unconfirmed |
| Technique Banner | "REALITY CHECK IDEAS" section under the card — swipeable glass banner with a random technique for *how* to test reality (nose pinch, finger count, re-reading text…); seeded with 20 classic methods, user-editable |
| Dream Stats | Streaks (current/best, read/done), totals with completion %, per-trigger leaderboard, last-14-nights dot timeline |
| Advice Banner | Swipeable banner on the main screen showing random advice for the active mode (Day or Night) |
| Settings Screen | Secondary screen for vibration & watch tuning, presets, snooze, advice and trigger management |
| Day/Night Toggle | Glass pill ☀ Day / ☾ Night toggle; each mode stores independent settings |
| Master Toggle | Switch to enable/disable vibration reminders (Settings screen) |
| Node Selector | Dropdown listing connected Wear OS nodes; select the "active watch" or "All watches" (Settings screen) |
| Frequency Sliders | Min/max interval sliders (1–120 min); min is clamped ≤ max (Settings screen) |
| Intensity Slider | Vibration motor intensity 1–255 (Settings screen) |
| Snooze Buttons | 15 min / 30 min / 1 hr quick-snooze buttons (Settings screen) |
| Preset Section | Save current settings as a named preset; load or delete saved presets (Settings screen) |

#### Reality check insistence loop

```
8 AM DailyTriggerReceiver ──► ensureTodayLog() ──► ongoing notification ──► arm NagScheduler
                                                                        │
              ┌─────────────────────────────────────────────────────────┘
              ▼
      NagReceiver (every 30 min)
              │ incomplete? ──► re-post notification ──► re-arm
              │ complete?   ──► cancel notification + disarm
              ▼
      User confirms via notification actions, or in-app
      RealityCheckCard ("I read it" / "I did it")
```

#### Multi-watch support
`WearSyncManager.pushSettings()` calls `DataClient.putDataItem()` which the Wearable Data Layer automatically delivers to **all** currently connected nodes and queues for nodes that are offline. When a disconnected watch reconnects, it receives the latest settings automatically.

To designate only one watch as the active vibrator, select it in the **Node Selector** dropdown. The phone pushes `KEY_TARGET_NODE_ID` = `<nodeId>` and the Wear app on each watch checks whether its own node ID matches before scheduling alarms.

---

## Data Flow

```
Phone UI change
    │
    ▼
MainViewModel.update*()
    │
    ├─► AppSettingsRepository.save()   (local DataStore)
    │
    └─► WearSyncManager.pushSettings() (DataClient.putDataItem)
              │
              ▼
        Wearable Data Layer
              │
    ┌─────────┴──────────┐
    ▼                    ▼
Watch A                Watch B
VibeDataListenerService  VibeDataListenerService
    │                    │
    ▼                    ▼
VibeSettingsRepository  VibeSettingsRepository
    │                    │
    ▼                    ▼
VibeScheduler           VibeScheduler
(checks target_node_id) (checks target_node_id)
```

---

## Dependencies

| Library | Used in |
|---------|---------|
| Jetpack Compose + Material 3 | `:app` |
| `lifecycle-viewmodel-compose` | `:app` |
| `datastore-preferences` | `:app` |
| `play-services-wearable` | `:app`, `:wear` |
| `kotlinx-coroutines-play-services` | `:app`, `:wear` |
| `kotlinx-serialization-json` 1.7.3 | `:app` |
| `wear-compose-material` 1.3.0 | `:wear` |
| `wear-compose-foundation` 1.3.0 | `:wear` |

---

## Changelog

### 2026-08-22T12:06 UTC
- **Dream sky: star-driven color, shake shatter, tilt-axis fix** ([`AccelerometerEffect.kt`](app/src/main/java/com/example/vild/ui/dream/AccelerometerEffect.kt), [`DreamBackground.kt`](app/src/main/java/com/example/vild/ui/dream/DreamBackground.kt)):
  - **Fix:** the accelerometer X axis was inverted — tilting the phone's *left* edge down sent stars to the right. X is now negated so stars always slide toward the downhill edge (Y was already correct).
  - **Predictive sky color:** the fixed 11 s hue timer is gone. Every star that newly enters the sky is tinted with the color the sky will wear *next*; after 40 new arrivals the sky crossfades to that color. Star speed is now strictly proportional to tilt — flat phone = still stars = no new arrivals = frozen color; steep phone = fast stream = quick color changes.
  - **Shake to shatter:** new `TiltState.shake` envelope (deviation of total acceleration from pure gravity, so pure tilt never triggers it). A sustained rapid shake explodes every star outward from the center and bleeds the sky to black (nebula orbs and glow fade too). The sky stays starless and black until new stars drift back in through the uphill edge — faster the steeper the phone — and 40 arrivals relight it in the newly predicted color.

### 2026-08-22T13:42 UTC
- **Build hygiene:** root `./gradlew installDebug` also runs `:wear:installDebug`, and since both modules share `applicationId = com.example.vild`, the wear APK **replaces the phone app** when a watch isn't connected (it did, briefly). Restored the phone app with a module-scoped install — **use `./gradlew :app:installDebug` for phone-only installs**; `:wear:installDebug` is only for when a watch is the connected device.

### 2026-08-22T10:25 UTC
- **Tail backfill bug fix — phantom "done" point:** logcat showed that during setup the READ slot was briefly mapped to the *Done* habit; its auto-backfill wrote today's read point into "Reality Check Done", and nothing ever cleared it (backfill only ever sent positive values).
  - [`MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): `backfillTail()` is now **fully authoritative** — for every day VILD has a log it sends the true read value (1/0) and done-round count (`maxOf(1, doneCount)`/0). Tail treats 0 as "clear this date", so wrong points self-heal on the next Send. Also fixes legacy logs (`doneAt` set before `doneCount` existed) being silently excluded from the done backfill. Days without a VILD log are untouched.
  - [`TailSection.kt`](app/src/main/java/com/example/vild/ui/settings/TailSection.kt): habit picker hardened — the dialog title now names the slot ("Read"/"Done") with a one-line description of when it fires, and a habit already mapped to the other slot is disabled with a hint, so both slots can never point at the same habit.

### 2026-08-22T10:10 UTC
- [`RealityCheckCard.kt`](app/src/main/java/com/example/vild/ui/realitycheck/RealityCheckCard.kt): the card's content now fills it edge to edge — header on top, buttons/streaks at the bottom, and the trigger text region expands into **all** the space between (centered, scrolling only as a last-resort backup). Trigger text shrunk again in [`Type.kt`](app/src/main/java/com/example/vild/ui/theme/Type.kt) (22→20 sp) so it is more likely to fit whole.

### 2026-08-22T09:55 UTC
- **Tail habit integration (WAGS protocol):**
  - **New:** [`TailIntegrationRepository.kt`](app/src/main/java/com/example/vild/data/TailIntegrationRepository.kt): IPC with the Tail app — habit discovery via its Content Provider (`content://com.example.tail.provider/habits`), live increments via explicit permission-guarded `ACTION_INCREMENT_HABIT` broadcasts, and idempotent retroactive backfill via `ACTION_SET_HABIT_VALUES` (`{"yyyy-MM-dd": <count>}` JSON). Two slots: READ (fires once/day) and DONE (fires on every tap).
  - [`RealityCheckDayLog.kt`](app/src/main/java/com/example/vild/data/RealityCheckDayLog.kt): new `doneCount` field — "I did it" is now repeatable; the first tap sets `doneAt` (completing the day), every tap bumps the count and sends another Tail increment.
  - [`RealityCheckStatsRepository.kt`](app/src/main/java/com/example/vild/data/RealityCheckStatsRepository.kt): `markReadToday()` is once-only (returns null when already read → Tail dedup); `markDoneToday()` increments the round count on every call.
  - [`NotificationHelper.kt`](app/src/main/java/com/example/vild/data/NotificationHelper.kt): the notification now lives **all day** — ongoing, with "✓ I did it" always present (repeatable, shows `×N` round count); only the nagging stops once the day is complete. [`RealityCheckActionReceiver.kt`](app/src/main/java/com/example/vild/ipc/RealityCheckActionReceiver.kt) sends the matching Tail increment on every notification action and never cancels the notification.
  - **New:** [`TailSection.kt`](app/src/main/java/com/example/vild/ui/settings/TailSection.kt): settings section with two habit slots, a searchable/sorted habit picker dialog (WAGS-style), Refresh, and a **backfill** button pushing today's + all past days' read/done values to Tail (connecting a habit backfills automatically). Wired into [`SettingsScreen.kt`](app/src/main/java/com/example/vild/ui/settings/SettingsScreen.kt) via new `TailUiState`/APIs in [`MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt).
- **Dream polish, round 2:**
  - [`MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt) + [`RealityCheckCard.kt`](app/src/main/java/com/example/vild/ui/realitycheck/RealityCheckCard.kt): the main card now absorbs **all** free vertical space (content floats centered inside it) — no empty gaps above/below.
  - [`DreamBackground.kt`](app/src/main/java/com/example/vild/ui/dream/DreamBackground.kt): stars are slightly larger and now **slide along the phone's angle** like dust on glass — heading steers toward the downhill direction of the lean, speed rises with steepness, and new stars enter from the uphill edge. Sky palette widened to 9 hues (teal, green, sky blue, gold, pink, coral, cyan, ember, lavender — new colors in [`Color.kt`](app/src/main/java/com/example/vild/ui/theme/Color.kt)) with no dominant color.
  - [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml): MainActivity locked to portrait.

### 2026-08-22T09:30 UTC
- **Dream polish pass:**
  - [`DreamBackground.kt`](app/src/main/java/com/example/vild/ui/dream/DreamBackground.kt): stars now drift in straight lines via a `withFrameNanos` physics loop; the accelerometer acts as a gentle wind that *bends* their trajectories (tilt → curved paths, level → straight); stars leaving the screen respawn from random edges heading inward, so the sky replenishes itself. The background hue constantly cross-fades to a new random dream color every 11 s (`animateColorAsState` + rotating palette), tinting the gradient, the lead nebula orb and the breathing glow.
  - [`AdviceBanner.kt`](app/src/main/java/com/example/vild/ui/advice/AdviceBanner.kt): restyled as a glass card — sibling of the technique banner but with its own voice (✦ glyph, rose `DreamPink` accents, `Mist` text). Tappable ‹ › arrows step through advice; swipe still works; tap opens notes.
  - [`TechniqueBanner.kt`](app/src/main/java/com/example/vild/ui/technique/TechniqueBanner.kt): "‹ swipe ›" hint replaced by tappable ‹ › arrows; in-card label "✧ reality check idea ✧" replaces the outer section label.
  - [`Type.kt`](app/src/main/java/com/example/vild/ui/theme/Type.kt): display typography shrunk (30→22 sp / 24→19 sp) so the main screen fits without scrolling.
  - [`RealityCheckCard.kt`](app/src/main/java/com/example/vild/ui/realitycheck/RealityCheckCard.kt): tighter padding; overly long trigger text now scrolls *inside* the card (max 120 dp) while the screen layout stays fixed.
  - [`MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): main screen is now a single unscrolling screen — header on top, footer below, and the dream column (reality check + ideas + advice) centered in the remaining space.

### 2026-08-22T08:30 UTC
- **Reality check techniques (ideas banner):**
  - **New:** [`TechniqueItem.kt`](app/src/main/java/com/example/vild/data/TechniqueItem.kt): `@Serializable` technique with `isSeeded` flag distinguishing classics from user-added ones.
  - **New:** [`TechniqueRepository.kt`](app/src/main/java/com/example/vild/data/TechniqueRepository.kt): DataStore CRUD + `seedIfEmpty()` which inserts the 20 most popular reality check methods (finger counting, nose pinch, re-reading text, light switches, mirrors, palm push, jumping, dream math…) exactly once — guarded by a `has_seeded` flag so deleting classics doesn't resurrect them.
  - **New:** [`TechniqueBanner.kt`](app/src/main/java/com/example/vild/ui/technique/TechniqueBanner.kt): dream-styled glass banner in its own "REALITY CHECK IDEAS" section on the main screen; swipe left → next random technique, right → previous; randomizes on app open.
  - **New:** [`TechniqueDialog.kt`](app/src/main/java/com/example/vild/ui/technique/TechniqueDialog.kt): management dialog (add/edit/delete, ✦ marks classics), opened from a new "REALITY CHECK TECHNIQUES" card in Settings.
  - [`MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): `TechniqueUiState` (list + index + history), seeding & observation on init, `randomizeTechnique`/`nextRandomTechnique`/`previousTechnique`/`addTechnique`/`updateTechnique`/`deleteTechnique`.
  - [`MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): banner section between the reality check card and the advice whisper.

### 2026-08-22T06:00 UTC
- **Reality-check-first redesign + dream aesthetic:**
  - **New daily practice core:** [`RealityCheckDayLog.kt`](app/src/main/java/com/example/vild/data/RealityCheckDayLog.kt) (per-day record with `readAt`/`doneAt`), [`RealityCheckStatsRepository.kt`](app/src/main/java/com/example/vild/data/RealityCheckStatsRepository.kt) (DataStore persistence, `ensureTodayLog`), [`RealityCheckStats.kt`](app/src/main/java/com/example/vild/data/RealityCheckStats.kt) (pure streak/leaderboard computation).
  - **Insistence loop:** [`NagScheduler.kt`](app/src/main/java/com/example/vild/data/NagScheduler.kt) + [`NagReceiver.kt`](app/src/main/java/com/example/vild/ipc/NagReceiver.kt) re-post the notification every 30 minutes until the check is confirmed; [`RealityCheckActionReceiver.kt`](app/src/main/java/com/example/vild/ipc/RealityCheckActionReceiver.kt) handles "✓ Read"/"✓ Done" notification actions; [`NotificationHelper.kt`](app/src/main/java/com/example/vild/data/NotificationHelper.kt) now builds an **ongoing** (un-dismissable) notification with status-aware title. [`BootReceiver.kt`](app/src/main/java/com/example/vild/ipc/BootReceiver.kt) re-arms the nag after reboot.
  - **Dream UI:** [`DreamBackground.kt`](app/src/main/java/com/example/vild/ui/dream/DreamBackground.kt) (accelerometer-parallaxed nebula orbs, twinkling starfield, breathing glow), [`AccelerometerEffect.kt`](app/src/main/java/com/example/vild/ui/dream/AccelerometerEffect.kt), [`GlassCard.kt`](app/src/main/java/com/example/vild/ui/dream/GlassCard.kt), new violet/aurora/serif theme in [`Color.kt`](app/src/main/java/com/example/vild/ui/theme/Color.kt)/[`Theme.kt`](app/src/main/java/com/example/vild/ui/theme/Theme.kt)/[`Type.kt`](app/src/main/java/com/example/vild/ui/theme/Type.kt).
  - **Main screen:** [`RealityCheckCard.kt`](app/src/main/java/com/example/vild/ui/realitycheck/RealityCheckCard.kt) shows today's check immediately on open with read/done confirmations and streaks; vibration settings moved to [`SettingsScreen.kt`](app/src/main/java/com/example/vild/ui/settings/SettingsScreen.kt); new [`StatsScreen.kt`](app/src/main/java/com/example/vild/ui/stats/StatsScreen.kt) (streaks, totals, trigger leaderboard, 14-night timeline). Screens cross-fade; no top app bar.
  - [`MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): added `allLogs`/`todayLog`/`stats` flows, `markRead()`/`markDone()` with notification + nag management, and `ensureTodayLog` on init.

### 2026-03-31T17:45 UTC
- **Settings screen & Advice feature** (adapted from wags project):
  - **New:** [`app/src/main/java/com/example/vild/data/AdviceItem.kt`](app/src/main/java/com/example/vild/data/AdviceItem.kt): `@Serializable` data class for advice items with `id`, `section` ("day"/"night"), `text`, and `createdAt` fields.
  - **New:** [`app/src/main/java/com/example/vild/data/AdviceRepository.kt`](app/src/main/java/com/example/vild/data/AdviceRepository.kt): DataStore-backed repository for advice CRUD. Stores all advice as JSON in a separate `advice_store` DataStore. Provides reactive `Flow` observation per section.
  - **New:** [`app/src/main/java/com/example/vild/ui/advice/AdviceSection.kt`](app/src/main/java/com/example/vild/ui/advice/AdviceSection.kt): Constants for the two advice sections (`DAY`, `NIGHT`) with labels and `all` list.
  - **New:** [`app/src/main/java/com/example/vild/ui/advice/AdviceBanner.kt`](app/src/main/java/com/example/vild/ui/advice/AdviceBanner.kt): Swipeable banner composable with `AnimatedContent` transitions. Swipe left → next random advice, swipe right → previous. Hidden when no advice exists. Max 5 visible lines with silent vertical scroll.
  - **New:** [`app/src/main/java/com/example/vild/ui/advice/AdviceDialog.kt`](app/src/main/java/com/example/vild/ui/advice/AdviceDialog.kt): Full-screen dialog for managing advice per section — add new advice, inline edit existing items, delete items.
  - **New:** [`app/src/main/java/com/example/vild/ui/settings/SettingsScreen.kt`](app/src/main/java/com/example/vild/ui/settings/SettingsScreen.kt): Settings screen with back navigation, containing an Advice card that lists Day and Night sections with item counts and Manage/Add buttons.
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `AdviceUiState` data class, `adviceRepo`, `_adviceState`/`adviceState` flows. Added advice methods: `randomizeAdvice()`, `nextRandomAdvice()`, `previousAdvice()`, `addAdvice()`, `updateAdvice()`, `deleteAdvice()`. Updated `toggleMode()` to randomize advice for the incoming mode. Advice randomizes on app start.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added gear icon (`Icons.Default.Settings`) in the top bar that toggles a `SettingsScreen` overlay. Added `AdviceBanner` at the top of the main content area, showing advice for the active mode (day/night). Collects `adviceState` from the ViewModel.

### 2026-03-31T02:06 UTC
- **Greyscale UI overhaul:**
  - [`app/src/main/java/com/example/vild/ui/theme/Color.kt`](app/src/main/java/com/example/vild/ui/theme/Color.kt): Replaced all purple/pink Material colours with a 13-step greyscale palette (Black → White).
  - [`app/src/main/java/com/example/vild/ui/theme/Theme.kt`](app/src/main/java/com/example/vild/ui/theme/Theme.kt): Replaced dynamic/light/dark colour schemes with a single fully-greyscale `darkColorScheme` that overrides every Material 3 colour role (primary, secondary, tertiary, error, surface, background, outline, inverse, scrim). Removed `dynamicColor` and `darkTheme` parameters from `VILDTheme`.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Replaced colour emojis (☀️ → ☼, 🌙 → ☽) in the Day/Night toggle with Unicode text symbols that render in the current text colour (greyscale). The background `Image` retains its original colours.
  - No changes needed to `VibrationSection.kt`, `SnoozeSection.kt`, or `PresetSection.kt` — they reference colours exclusively through `MaterialTheme.colorScheme.*`, which is now greyscale.

### 2026-03-31T01:43 UTC
- **Custom app icon & branding:**
  - Replaced default Android robot launcher icon with custom VILD woven-knot pattern icon (`VILD_icon.png`) for both `:app` and `:wear` modules.
  - Generated adaptive icon foreground PNGs at all density buckets (mdpi through xxxhdpi) in `drawable-*` folders, with a dark `#1A1A2E` vector background layer.
  - Generated legacy `ic_launcher.webp` and `ic_launcher_round.webp` at all densities for both modules.
  - Updated [`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml) to reference `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.
  - Created `vild_background.webp` (1080×1920) from the center pattern of the icon, used as a non-scrolling background in the phone app UI at 15% opacity.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Wrapped scrollable `Column` in a `Box` with a fixed `Image` background behind the content.

### 2026-03-29T23:23 UTC
- **Bug fix – scheduled vibrations never repeating (Doze rate-limiting):**
  - **Root cause:** `setExactAndAllowWhileIdle()` has a system-enforced minimum interval of ~10 minutes on API 31+ (targetSdk 36). When the user set 1–2 minute intervals, the first alarm fired but subsequent ones were silently deferred or dropped by the OS. Combined with the BroadcastReceiver context issues from the previous fix, the alarm chain was permanently broken.
  - **Fix – Switched to `setAlarmClock()`** ([`VibeScheduler.kt`](wear/src/main/java/com/example/vild/wear/VibeScheduler.kt:80)): `AlarmManager.setAlarmClock()` is exempt from all Doze mode rate-limiting and always fires at the exact requested time. This allows reminder intervals as short as 1 minute. The trade-off is a small alarm icon in the watch status bar, which is appropriate for a reminder app.
  - Retained all previous fixes: `applicationContext` usage, 10s WakeLock, 5s Play Services timeout, emergency reschedule on failure.

### 2026-03-29T23:08 UTC
- **Bug fix – alarm chain context issues:**
  - **Root cause:** `VibeReceiver` passed its short-lived BroadcastReceiver context to Play Services calls, and the 3-second WakeLock was too short.
  - **Fix 1 – Use `applicationContext` everywhere** ([`VibeReceiver.kt`](wear/src/main/java/com/example/vild/wear/VibeReceiver.kt), [`VibeScheduler.kt`](wear/src/main/java/com/example/vild/wear/VibeScheduler.kt), [`VibeDataListenerService.kt`](wear/src/main/java/com/example/vild/wear/VibeDataListenerService.kt), [`BootReceiver.kt`](wear/src/main/java/com/example/vild/wear/BootReceiver.kt)): All callers now pass `context.applicationContext`.
  - **Fix 2 – Extended WakeLock to 10 seconds** ([`VibeReceiver.kt`](wear/src/main/java/com/example/vild/wear/VibeReceiver.kt:36)).
  - **Fix 3 – 5-second timeout on `getNodeClient().localNode`** ([`VibeScheduler.kt`](wear/src/main/java/com/example/vild/wear/VibeScheduler.kt:113)).
  - **Fix 4 – Emergency reschedule on failure** ([`VibeReceiver.kt`](wear/src/main/java/com/example/vild/wear/VibeReceiver.kt:44)).

### 2026-03-29T22:55 UTC
- **Bug fix – random timer never firing:**
  - **Root cause 1 – No alarm recovery after process death:** `VibeScheduler.schedule()` was only called from `VibeDataListenerService.onDataChanged()` and `VibeReceiver.onReceive()`. If the watch process was killed by the OS (common on Wear OS), the `AlarmManager` alarm was lost permanently until the phone pushed new settings.
  - **Root cause 2 – No alarm recovery after reboot:** `AlarmManager` alarms are cleared on device reboot. There was no `BOOT_COMPLETED` receiver to re-establish the alarm.
  - **Root cause 3 – Silent failure in `isThisNodeTargeted()`:** If `Wearable.getNodeClient().localNode.await()` threw an exception (e.g., Play Services temporarily unavailable), the entire `schedule()` call was silently swallowed by the outer try-catch, leaving no alarm scheduled.
  - **Fix 1 – `MainActivity.onCreate()` now calls `VibeScheduler.schedule()`** ([`wear/src/main/java/com/example/vild/wear/MainActivity.kt`](wear/src/main/java/com/example/vild/wear/MainActivity.kt)): Every time the user opens the watch app, the alarm is re-established. This is the primary safety net for process-death scenarios.
  - **Fix 2 – New `BootReceiver`** ([`wear/src/main/java/com/example/vild/wear/BootReceiver.kt`](wear/src/main/java/com/example/vild/wear/BootReceiver.kt)): Listens for `ACTION_BOOT_COMPLETED` and calls `VibeScheduler.schedule()` so the alarm survives device reboots.
  - **Fix 3 – `isThisNodeTargeted()` now defaults to `true` on exception** ([`wear/src/main/java/com/example/vild/wear/VibeScheduler.kt`](wear/src/main/java/com/example/vild/wear/VibeScheduler.kt)): If the node client call fails, scheduling proceeds rather than silently aborting.
  - **Manifest updates** ([`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml)): Added `RECEIVE_BOOT_COMPLETED` permission and registered `BootReceiver` with `BOOT_COMPLETED` intent filter.

### 2026-03-29T22:00 UTC
- **Phase 4 – Day/Night Mode:**
  - [`app/src/main/java/com/example/vild/data/AppSettingsRepository.kt`](app/src/main/java/com/example/vild/data/AppSettingsRepository.kt): Added `@Serializable` to `VibeSettings` (required for JSON mode snapshots). Added `keyActiveMode`, `keyDaySettings`, `keyNightSettings` DataStore keys. Added `activeModeFlow: Flow<String>` (emits `"day"` or `"night"`). Added `saveModeSettings(mode, settings)`, `loadModeSettings(mode): VibeSettings`, and `setActiveMode(mode)`.
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `_activeMode: MutableStateFlow<String>` and exposed `activeMode: StateFlow<String>`. Added `toggleMode()` — saves current settings under the outgoing mode, switches `activeMode` in DataStore, loads the incoming mode's settings, and syncs to the watch. Updated `loadPreset()` to also call `repo.saveModeSettings(activeMode, newSettings)` so the preset is persisted under the active mode's key.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added `DayNightToggle` composable — a two-button segmented row (☀️ Day / 🌙 Night) placed directly below the `SyncStatusBar`. The active mode button is filled (Night uses `secondary` color); the inactive one is outlined. Clicking the inactive button calls `vm.toggleMode()`.

### 2026-03-29T21:56 UTC
- **Phase 3 – Named Presets:**
  - **New:** [`app/src/main/java/com/example/vild/data/Preset.kt`](app/src/main/java/com/example/vild/data/Preset.kt): `@Serializable` data class capturing a named snapshot of vibration/scheduling settings (`name`, `isEnabled`, `freqMinMinutes`, `freqMaxMinutes`, `vibrationIntensity`, `vibrationDurationMs`, `vibrationPatternType`, `vibrationRepeatCount`). Excludes transient fields (`snoozeUntilTimestamp`, `targetNodeId`, `customSnoozeDurations`).
  - [`app/src/main/java/com/example/vild/data/AppSettingsRepository.kt`](app/src/main/java/com/example/vild/data/AppSettingsRepository.kt): Added `keyPresets = stringPreferencesKey("presets_json")`. Added `presetsFlow: Flow<List<Preset>>` (deserializes JSON from DataStore). Added `savePreset(preset)` (upserts by name) and `deletePreset(name)` (removes by name).
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `presets: StateFlow<List<Preset>>` collected from `repo.presetsFlow`. Added `saveCurrentAsPreset(name)`, `loadPreset(preset)` (applies preset fields and syncs to watch), and `deletePreset(name)`.
  - **New:** [`app/src/main/java/com/example/vild/ui/PresetSection.kt`](app/src/main/java/com/example/vild/ui/PresetSection.kt): Composable section with a "Save current settings as preset" button (opens name dialog), a list of saved presets each with Load/Delete actions, and a delete confirmation dialog. Shows "No saved presets" placeholder when the list is empty.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added `PresetSection` between the Vibration and Snooze sections.

### 2026-03-29T21:54 UTC
- **Phase 2 – Sync Status Indicator:**
  - [`app/src/main/java/com/example/vild/data/WearSyncManager.kt`](app/src/main/java/com/example/vild/data/WearSyncManager.kt): `pushSettings()` now returns `Boolean` — `true` on success, `false` if an exception is thrown.
  - [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt): Added `SyncStatus` data class (`lastSyncTimestamp: Long`, `lastSyncSuccess: Boolean`). Added `_syncStatus: MutableStateFlow<SyncStatus>` and exposed `syncStatus: StateFlow<SyncStatus>`. `updateAndSync()` now captures the `Boolean` result from `pushSettings()` and updates `_syncStatus` with the current timestamp and success flag.
  - [`app/src/main/java/com/example/vild/MainActivity.kt`](app/src/main/java/com/example/vild/MainActivity.kt): Added `SyncStatusBar` composable — a slim `Surface` banner at the top of the content area. Hidden until the first sync attempt. Shows green `primaryContainer` + "✓ Synced Xs ago" on success; red `errorContainer` + "✗ Sync failed" on failure. `VildApp` collects `vm.syncStatus` and passes it to `SyncStatusBar`.

### 2026-03-29T21:51 UTC
- **Phase 5 (prereq):** Added `kotlinx-serialization-json:1.7.3` dependency to [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`app/build.gradle.kts`](app/build.gradle.kts). Also added the `kotlin-serialization` Gradle plugin to both files (required for `@Serializable` annotation processing in future phases).
- **Phase 1 – Duration slider:** Increased vibration duration slider max from 2000ms → 4000ms in [`app/src/main/java/com/example/vild/ui/VibrationSection.kt`](app/src/main/java/com/example/vild/ui/VibrationSection.kt). Updated `steps` from 37 → 77 to maintain 50ms granularity.
- **Phase 1 – Cancel snooze:** Added `cancelSnooze()` to [`app/src/main/java/com/example/vild/MainViewModel.kt`](app/src/main/java/com/example/vild/MainViewModel.kt) — resets `snoozeUntilTimestamp` to 0 and syncs to watch. Added a "Cancel snooze" `TextButton` in [`app/src/main/java/com/example/vild/ui/SnoozeSection.kt`](app/src/main/java/com/example/vild/ui/SnoozeSection.kt) that appears inline with the countdown text only when a snooze is active.

### 2026-03-29T21:31 UTC
- Replaced `finish()`-only [`wear/src/main/java/com/example/vild/wear/MainActivity.kt`](wear/src/main/java/com/example/vild/wear/MainActivity.kt) with a persistent Wear OS Compose UI (`ComponentActivity` + `setContent`). The screen displays "VILD is active. Configure settings from your phone." — keeping the activity alive so the OS does not kill `VibeDataListenerService`.
- Added a **Test Vibration** button to the watch UI that calls `VibrationHelper.vibrate(context)` directly, allowing on-device verification of the vibrator.
- Added `androidx-activity-compose` dependency to [`wear/build.gradle.kts`](wear/build.gradle.kts) (required for `setContent` in `ComponentActivity`).
- Confirmed `android.permission.VIBRATE` is present in [`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml) ✅
- Confirmed `VibeDataListenerService` is registered with both `DATA_CHANGED` and `MESSAGE_RECEIVED` intent filters in the wear manifest ✅

### 2026-03-29T21:00 UTC
- Added `alias(libs.plugins.android.library) apply false` to root [`build.gradle.kts`](build.gradle.kts) — fixes "plugin already on classpath with unknown version" Gradle error for the `:shared` module.
- Replaced non-existent `wear-compose-bom:2024.10.00` with explicit `wear-compose-material:1.3.0` and `wear-compose-foundation:1.3.0` versions in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`wear/build.gradle.kts`](wear/build.gradle.kts).
- Added `kotlinx-coroutines-play-services:1.9.0` dependency to both `:app` and `:wear` modules to resolve `kotlinx.coroutines.tasks.await` unresolved reference.
- Added minimal [`wear/src/main/java/com/example/vild/wear/MainActivity.kt`](wear/src/main/java/com/example/vild/wear/MainActivity.kt) with `LAUNCHER` intent filter so the `:wear` module appears in Android Studio run configurations.
- Removed missing `@mipmap/ic_launcher` reference from [`wear/src/main/AndroidManifest.xml`](wear/src/main/AndroidManifest.xml).
- Build verified: `./gradlew assembleDebug` → **BUILD SUCCESSFUL** in 16s.
