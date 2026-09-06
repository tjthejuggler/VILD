package com.example.vild

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vild.data.AdviceItem
import com.example.vild.data.AdviceRepository
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.DailyTriggerScheduler
import com.example.vild.data.NagScheduler
import com.example.vild.data.NightVibeLogRepository
import com.example.vild.data.NightVibeNotifier
import com.example.vild.data.NightVibeScheduler
import com.example.vild.data.NightVibeSettings
import com.example.vild.data.NotificationHelper
import com.example.vild.data.RealityCheckDayLog
import com.example.vild.data.RealityCheckRepository
import com.example.vild.data.RealityCheckStats
import com.example.vild.data.RealityCheckStatsRepository
import com.example.vild.data.RealityCheckTrigger
import com.example.vild.data.TailHabit
import com.example.vild.data.TailIntegrationRepository
import com.example.vild.data.TechniqueItem
import com.example.vild.data.TechniqueRepository
import com.example.vild.data.computeStats
import com.example.vild.data.todayEpochDay
import com.example.vild.ui.advice.AdviceSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

private const val TAG = "MainViewModel"

/**
 * UI state for the advice feature.
 *
 * @property adviceBySection All advice items keyed by section ("day" / "night").
 * @property currentIndex    Current random-index per section for the banner display.
 * @property history         History of shown indices per section (for swipe-back).
 */
data class AdviceUiState(
    val adviceBySection: Map<String, List<AdviceItem>> = emptyMap(),
    val currentIndex: Map<String, Int> = emptyMap(),
    val history: Map<String, List<Int>> = emptyMap(),
)

/**
 * UI state for the reality check techniques feature.
 *
 * @property techniques  All technique items (seeded classics + user-added).
 * @property currentIndex Current random-index for the banner display.
 * @property history      History of shown indices (for swipe-back).
 */
data class TechniqueUiState(
    val techniques: List<TechniqueItem> = emptyList(),
    val currentIndex: Int = 0,
    val history: List<Int> = emptyList(),
)

/**
 * UI state for the Tail habit-tracker integration.
 *
 * @property habits      Habits exposed by Tail's Content Provider.
 * @property loading     True while a habit fetch is in flight.
 * @property unavailable True when Tail is not installed / exposes no habits.
 * @property readHabit   Selected Tail habit name for the "read" event ("" = none).
 * @property doneHabit   Selected Tail habit name for the "done" event ("" = none).
 * @property backfilling True while the retroactive backfill broadcast is queued.
 * @property message     Last success message (dismissable).
 * @property error       Last error (dismissable).
 */
data class TailUiState(
    val habits: List<TailHabit> = emptyList(),
    val loading: Boolean = false,
    val unavailable: Boolean = false,
    val readHabit: String = "",
    val doneHabit: String = "",
    val backfilling: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

/**
 * Holds all UI state for [MainActivity].
 *
 * Night vibes are plain phone notifications mirrored to whatever wearable is
 * paired (e.g. a Garmin watch) — no wearable-specific code. Every settings
 * change persists locally and re-arms the [NightVibeScheduler] alarm chain.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppSettingsRepository(application)
    private val nightLogRepo = NightVibeLogRepository(application)
    private val adviceRepo = AdviceRepository(application)
    private val triggerRepo = RealityCheckRepository(application)
    private val statsRepo = RealityCheckStatsRepository(application)
    private val techniqueRepo = TechniqueRepository(application)
    private val tailRepo = TailIntegrationRepository(application)

    // ── UI state ─────────────────────────────────────────────────────────────

    private val _settings = MutableStateFlow(NightVibeSettings())
    val settings: StateFlow<NightVibeSettings> = _settings.asStateFlow()

    /** Epoch-ms timestamps of every night-vibe notification sent (oldest → newest). */
    val nightVibeLog: StateFlow<List<Long>> = nightLogRepo.sentTimesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** `"day"` or `"night"` — persisted in DataStore. */
    private val _activeMode = MutableStateFlow("day")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    // ── Tail integration state ────────────────────────────────────────────────

    /** True if the Tail habit-tracker app is installed on this device. */
    val isTailInstalled: Boolean = try {
        application.packageManager.getPackageInfo("com.example.tail", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private val _autoSwitchDayOnHabit = MutableStateFlow(false)
    val autoSwitchDayOnHabit: StateFlow<Boolean> = _autoSwitchDayOnHabit.asStateFlow()

    private val _tailState = MutableStateFlow(
        TailUiState(
            readHabit = tailRepo.getHabitName(TailIntegrationRepository.Slot.READ),
            doneHabit = tailRepo.getHabitName(TailIntegrationRepository.Slot.DONE),
        ),
    )
    val tailState: StateFlow<TailUiState> = _tailState.asStateFlow()

    // ── Advice state ──────────────────────────────────────────────────────────

    private val _adviceState = MutableStateFlow(AdviceUiState())
    val adviceState: StateFlow<AdviceUiState> = _adviceState.asStateFlow()

    // ── Reality check techniques state ─────────────────────────────────────────

    private val _techniqueState = MutableStateFlow(TechniqueUiState())
    val techniqueState: StateFlow<TechniqueUiState> = _techniqueState.asStateFlow()

    // ── Reality check trigger state ────────────────────────────────────────────

    val triggers: StateFlow<List<RealityCheckTrigger>> = triggerRepo.allTriggersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── Reality check daily log & stats state ──────────────────────────────────

    /** All day logs, oldest → newest. */
    val allLogs: StateFlow<List<RealityCheckDayLog>> = statsRepo.allLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Today's log — the primary object on the main screen. */
    val todayLog: StateFlow<RealityCheckDayLog?> = allLogs
        .map { logs -> logs.firstOrNull { it.epochDay == todayEpochDay() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** Derived stats (streaks, totals, per-trigger leaderboard). */
    val stats: StateFlow<RealityCheckStats> = allLogs
        .map { computeStats(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RealityCheckStats(),
        )

    /**
     * Live countdown text derived from [NightVibeSettings.snoozeUntilTimestamp].
     * Emits "Snoozed — X min Y sec remaining" while snoozed, null otherwise.
     * Ticks every second.
     */
    val snoozeCountdownText: StateFlow<String?> = flow {
        while (true) {
            val until = _settings.value.snoozeUntilTimestamp
            val remaining = until - System.currentTimeMillis()
            if (remaining > 0) {
                val mins = (remaining / 60_000).toInt()
                val secs = ((remaining % 60_000) / 1_000).toInt()
                emit("Snoozed — ${mins}m ${secs}s remaining")
            } else {
                emit(null)
            }
            delay(1_000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            _activeMode.value = repo.activeModeFlow.first()
            _settings.value = repo.settingsFlow.first()
            _autoSwitchDayOnHabit.value = repo.autoSwitchDayOnHabitFlow.first()
        }
        // Observe advice for both sections
        AdviceSection.all.forEach { section ->
            viewModelScope.launch {
                adviceRepo.observeBySection(section).collect { list ->
                    _adviceState.update { old ->
                        val newMap = old.adviceBySection.toMutableMap()
                        newMap[section] = list
                        val idx = old.currentIndex[section] ?: 0
                        val newIdx = if (list.isEmpty()) 0 else idx.coerceIn(0, list.size - 1)
                        val newIndexMap = old.currentIndex.toMutableMap()
                        newIndexMap[section] = newIdx
                        old.copy(adviceBySection = newMap, currentIndex = newIndexMap)
                    }
                }
            }
        }
        // Randomize advice for the active mode on app start
        viewModelScope.launch {
            // Wait for advice to load
            delay(500)
            randomizeAdvice(_activeMode.value)
        }
        // Seed the classic reality check techniques on first run, then observe
        viewModelScope.launch {
            techniqueRepo.seedIfEmpty()
            techniqueRepo.allTechniquesFlow.collect { list ->
                _techniqueState.update { old ->
                    val idx = old.currentIndex
                    val newIdx = if (list.isEmpty()) 0 else idx.coerceIn(0, list.size - 1)
                    old.copy(techniques = list, currentIndex = newIdx)
                }
            }
        }
        // Show a random technique on app start
        viewModelScope.launch {
            delay(700)
            randomizeTechnique()
        }
        // Ensure notification channels exist and schedule daily trigger alarm
        NotificationHelper.ensureChannel(application)
        NightVibeNotifier.ensureChannel(application)
        DailyTriggerScheduler.schedule(application)
        // Arm the night-vibe chain and keep it in sync with any settings change
        // (including snoozes made from other entry points).
        viewModelScope.launch {
            NightVibeScheduler.scheduleNext(application)
            repo.settingsFlow.collect { saved ->
                _settings.value = saved
                runCatching { NightVibeScheduler.scheduleNext(application) }
            }
        }
        // Make sure today's reality check exists the moment the app opens,
        // and arm the (adaptive) nag cycle if today's goals are unmet.
        viewModelScope.launch {
            runCatching {
                val triggers = triggerRepo.allTriggersFlow.first()
                val log = statsRepo.ensureTodayLog(triggers)
                NagScheduler.nextIntervalMs(log)?.let { interval ->
                    NotificationHelper.showNotification(application, log)
                    NagScheduler.schedule(application, interval)
                }
            }
        }
    }

    // ── Night vibes API ──────────────────────────────────────────────────────

    fun updateIsEnabled(enabled: Boolean) = updateSettings(_settings.value.copy(isEnabled = enabled))

    fun updateNightStart(minutesOfDay: Int) =
        updateSettings(_settings.value.copy(nightStartMinutes = minutesOfDay))

    fun updateGapMinutes(minutes: Int) =
        updateSettings(_settings.value.copy(gapMinutes = minutes))

    fun updateRemInterval(minutes: Int) =
        updateSettings(_settings.value.copy(remIntervalMinutes = minutes))

    /** Posts a test night-vibe notification immediately (not written to the log). */
    fun testNightVibe() {
        NightVibeNotifier.show(getApplication())
    }

    /**
     * Sets [NightVibeSettings.snoozeUntilTimestamp] to [System.currentTimeMillis] + [durationMs].
     */
    fun snooze(durationMs: Long) {
        val until = System.currentTimeMillis() + durationMs
        updateSettings(_settings.value.copy(snoozeUntilTimestamp = until))
    }

    /** Cancels any active snooze by resetting [NightVibeSettings.snoozeUntilTimestamp] to 0. */
    fun cancelSnooze() {
        updateSettings(_settings.value.copy(snoozeUntilTimestamp = 0L))
    }

    /** Adds a custom snooze duration (in ms) if not already present. */
    fun addCustomSnoozeDuration(durationMs: Long) {
        val current = _settings.value.customSnoozeDurations
        if (durationMs !in current) {
            updateSettings(_settings.value.copy(customSnoozeDurations = current + durationMs))
        }
    }

    /** Removes a custom snooze duration (in ms). */
    fun removeCustomSnoozeDuration(durationMs: Long) {
        val updated = _settings.value.customSnoozeDurations.filter { it != durationMs }
        updateSettings(_settings.value.copy(customSnoozeDurations = updated))
    }

    // ── Day/Night mode API ───────────────────────────────────────────────────

    /**
     * Toggles between Day and Night mode:
     * 1. Saves current settings under the outgoing mode.
     * 2. Switches [activeMode] to the other mode.
     * 3. Loads the incoming mode's settings.
     * 4. Updates [_settings] and re-arms the night-vibe chain.
     */
    fun toggleMode() {
        viewModelScope.launch {
            val outgoing = _activeMode.value
            val incoming = if (outgoing == "day") "night" else "day"

            // Save current settings under the outgoing mode
            repo.saveModeSettings(outgoing, _settings.value)

            // Switch mode in DataStore
            repo.setActiveMode(incoming)
            _activeMode.value = incoming

            // Load the incoming mode's settings and re-arm
            val incomingSettings = repo.loadModeSettings(incoming)
            updateSettings(incomingSettings)

            // Randomize advice for the incoming mode
            randomizeAdvice(incoming)
        }
    }

    // ── Tail integration API ─────────────────────────────────────────────────

    /** Persists the auto-switch-day-on-habit toggle. */
    fun setAutoSwitchDayOnHabit(enabled: Boolean) {
        _autoSwitchDayOnHabit.value = enabled
        viewModelScope.launch { repo.setAutoSwitchDayOnHabit(enabled) }
    }

    // ── Advice API ────────────────────────────────────────────────────────────

    /** Picks a fresh random index for [section]. */
    fun randomizeAdvice(section: String) {
        _adviceState.update { old ->
            val list = old.adviceBySection[section] ?: return@update old
            if (list.isEmpty()) return@update old
            val newIdx = (0 until list.size).random()
            val newIndexMap = old.currentIndex.toMutableMap()
            newIndexMap[section] = newIdx
            old.copy(currentIndex = newIndexMap)
        }
    }

    /** Swipe left → show next random advice (not the same as current). */
    fun nextRandomAdvice(section: String) {
        _adviceState.update { old ->
            val list = old.adviceBySection[section] ?: return@update old
            if (list.size <= 1) return@update old
            val currentIdx = old.currentIndex[section] ?: 0
            var newIdx: Int
            do {
                newIdx = (0 until list.size).random()
            } while (newIdx == currentIdx)
            val newIndexMap = old.currentIndex.toMutableMap()
            newIndexMap[section] = newIdx
            val sectionHistory = (old.history[section] ?: emptyList()) + currentIdx
            val newHistory = old.history.toMutableMap()
            newHistory[section] = sectionHistory
            old.copy(currentIndex = newIndexMap, history = newHistory)
        }
    }

    /** Swipe right → go back to previously shown advice. */
    fun previousAdvice(section: String) {
        _adviceState.update { old ->
            val sectionHistory = old.history[section] ?: return@update old
            if (sectionHistory.isEmpty()) return@update old
            val prevIdx = sectionHistory.last()
            val newHistory = old.history.toMutableMap()
            newHistory[section] = sectionHistory.dropLast(1)
            val newIndexMap = old.currentIndex.toMutableMap()
            newIndexMap[section] = prevIdx
            old.copy(currentIndex = newIndexMap, history = newHistory)
        }
    }

    fun addAdvice(section: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { adviceRepo.add(section, text.trim()) }
    }

    fun updateAdvice(item: AdviceItem, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch { adviceRepo.update(item, newText.trim()) }
    }

    fun deleteAdvice(id: Long) {
        viewModelScope.launch { adviceRepo.delete(id) }
    }

    fun updateAdviceNotes(id: Long, notes: String) {
        viewModelScope.launch { adviceRepo.updateNotes(id, notes) }
    }

    // ── Reality Check Trigger API ─────────────────────────────────────────────

    fun addTrigger(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { triggerRepo.add(text.trim()) }
    }

    fun updateTrigger(item: RealityCheckTrigger, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch { triggerRepo.update(item, newText.trim()) }
    }

    fun deleteTrigger(id: Long) {
        viewModelScope.launch { triggerRepo.delete(id) }
    }

    // ── Reality check techniques API ───────────────────────────────────────────

    /** Picks a fresh random technique index. */
    fun randomizeTechnique() {
        _techniqueState.update { old ->
            if (old.techniques.isEmpty()) return@update old
            old.copy(currentIndex = (0 until old.techniques.size).random())
        }
    }

    /** Swipe left → show next random technique (not the same as current). */
    fun nextRandomTechnique() {
        _techniqueState.update { old ->
            val list = old.techniques
            if (list.size <= 1) return@update old
            val currentIdx = old.currentIndex
            var newIdx: Int
            do {
                newIdx = (0 until list.size).random()
            } while (newIdx == currentIdx)
            old.copy(currentIndex = newIdx, history = old.history + currentIdx)
        }
    }

    /** Swipe right → go back to the previously shown technique. */
    fun previousTechnique() {
        _techniqueState.update { old ->
            if (old.history.isEmpty()) return@update old
            val prevIdx = old.history.last()
            old.copy(currentIndex = prevIdx, history = old.history.dropLast(1))
        }
    }

    fun addTechnique(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { techniqueRepo.add(text.trim()) }
    }

    fun updateTechnique(item: TechniqueItem, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch { techniqueRepo.update(item, newText.trim()) }
    }

    fun deleteTechnique(id: Long) {
        viewModelScope.launch { techniqueRepo.delete(id) }
    }

    // ── Reality check daily confirmation API ───────────────────────────────────

    /**
     * Records one more "I read it" round — deliberately repeatable. Every
     * single tap increments the Tail habit mapped to READ; the first tap is
     * what sets readAt (and helps complete the day).
     */
    fun markRead() = markToday { repo ->
        repo.markReadToday()?.also {
            tailRepo.sendHabitIncrement(TailIntegrationRepository.Slot.READ)
        }
    }

    /**
     * Records one more "I did it" round — deliberately repeatable. Every
     * single tap increments the Tail habit mapped to DONE; the first tap is
     * what completes the day (and stops the nagging).
     */
    fun markDone() = markToday { repo ->
        repo.markDoneToday()?.also {
            tailRepo.sendHabitIncrement(TailIntegrationRepository.Slot.DONE)
        }
    }

    // ── Tail habit-tracker integration API ─────────────────────────────────────

    /** Fetches the habit list from Tail's Content Provider. */
    fun refreshTailHabits() {
        _tailState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val habits = tailRepo.fetchHabits()
            _tailState.update {
                it.copy(
                    habits = habits,
                    loading = false,
                    unavailable = habits.isEmpty(),
                )
            }
        }
    }

    /** Persists the chosen habit for [slot] and backfills its history to Tail. */
    fun selectTailHabit(slot: TailIntegrationRepository.Slot, habit: TailHabit) {
        tailRepo.setHabit(slot, habit.habitName)
        _tailState.update {
            when (slot) {
                TailIntegrationRepository.Slot.READ -> it.copy(readHabit = habit.habitName)
                TailIntegrationRepository.Slot.DONE -> it.copy(doneHabit = habit.habitName)
            }
        }
        // Connecting a new habit backfills its history automatically (wags pattern).
        backfillTail()
    }

    /** Clears the habit selection for [slot]. */
    fun clearTailHabit(slot: TailIntegrationRepository.Slot) {
        tailRepo.clearHabit(slot)
        _tailState.update {
            when (slot) {
                TailIntegrationRepository.Slot.READ -> it.copy(readHabit = "")
                TailIntegrationRepository.Slot.DONE -> it.copy(doneHabit = "")
            }
        }
    }

    /**
     * Sends every logged day's read/done values to Tail with SET semantics —
     * fully authoritative and idempotent. Read days send the read-round count
     * (0 when the check was never read); done days send the done-round count
     * (0 when never done). Tail treats 0 as "clear this date", so a point
     * that was pushed to the wrong habit (e.g. while a slot was briefly
     * mis-mapped) is wiped on the next backfill. Days VILD has no log for
     * are left untouched.
     */
    fun backfillTail() {
        _tailState.update { it.copy(backfilling = true, message = null, error = null) }
        viewModelScope.launch {
            try {
                val logs = allLogs.value
                if (logs.isNotEmpty()) {
                    tailRepo.sendHabitValuesForDates(
                        TailIntegrationRepository.Slot.READ,
                        logs.associate {
                            // Legacy logs predate readCount: readAt set, count 0 → count as 1.
                            dateKey(it.epochDay) to
                                if (it.readAt != null) maxOf(1, it.readCount) else 0
                        },
                    )
                    tailRepo.sendHabitValuesForDates(
                        TailIntegrationRepository.Slot.DONE,
                        logs.associate {
                            // Legacy logs predate doneCount: doneAt set, count 0 → count as 1.
                            dateKey(it.epochDay) to
                                if (it.doneAt != null) maxOf(1, it.doneCount) else 0
                        },
                    )
                }
                _tailState.update {
                    it.copy(
                        backfilling = false,
                        message = "Backfill sent — ${logs.size} day(s) synced to Tail " +
                            "(not-done days cleared).",
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "backfillTail failed: ${e.message}")
                _tailState.update {
                    it.copy(backfilling = false, error = "Backfill failed — is Tail installed?")
                }
            }
        }
    }

    /** Dismisses the backfill message/error. */
    fun dismissTailMessage() {
        _tailState.update { it.copy(message = null, error = null) }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun markToday(action: suspend (RealityCheckStatsRepository) -> RealityCheckDayLog?) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val log = runCatching { action(statsRepo) }.getOrNull() ?: return@launch
            // The notification stays all day (with "I did it" always available for
            // extra Tail rounds) — but the nagging stops once the day is complete.
            NotificationHelper.showNotification(app, log)
            if (log.isComplete) {
                NagScheduler.cancel(app)
            }
        }
    }

    /** Converts an epoch day to the ISO-8601 date string Tail expects. */
    private fun dateKey(epochDay: Long): String =
        java.time.LocalDate.ofEpochDay(epochDay).toString()

    private fun updateSettings(newSettings: NightVibeSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            repo.save(newSettings)
            // repo.settingsFlow collector in init re-arms the scheduler.
        }
    }
}
