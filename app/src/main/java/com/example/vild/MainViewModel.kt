package com.example.vild

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vild.data.AdviceItem
import com.example.vild.data.AdviceRepository
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.Preset
import com.example.vild.data.VibeSettings
import com.example.vild.data.WearSyncManager
import com.example.vild.shared.VibeConstants
import com.example.vild.ui.advice.AdviceSection
import com.google.android.gms.wearable.Node
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
 * Represents the result of the most recent settings push to the Wearable Data Layer.
 *
 * @property lastSyncTimestamp epoch-ms of the last push attempt; 0 means never synced.
 * @property lastSyncSuccess   true if the last push succeeded, false if it threw.
 */
data class SyncStatus(
    val lastSyncTimestamp: Long = 0L,
    val lastSyncSuccess: Boolean = true,
)

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
 * Holds all UI state for [MainActivity].
 *
 * On init it loads the last saved settings from [AppSettingsRepository] and
 * fetches the list of connected Wear OS nodes via [WearSyncManager].
 *
 * Every public `update*` function persists the change locally **and** pushes
 * the full settings snapshot to the Wearable Data Layer.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppSettingsRepository(application)
    private val syncManager = WearSyncManager(application)
    private val adviceRepo = AdviceRepository(application)

    // ── UI state ─────────────────────────────────────────────────────────────

    private val _settings = MutableStateFlow(VibeSettings())
    val settings: StateFlow<VibeSettings> = _settings.asStateFlow()

    private val _nodes = MutableStateFlow<List<Node>>(emptyList())
    val nodes: StateFlow<List<Node>> = _nodes.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** `"day"` or `"night"` — persisted in DataStore. */
    private val _activeMode = MutableStateFlow("day")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    // ── Advice state ──────────────────────────────────────────────────────────

    private val _adviceState = MutableStateFlow(AdviceUiState())
    val adviceState: StateFlow<AdviceUiState> = _adviceState.asStateFlow()

    val presets: StateFlow<List<Preset>> = repo.presetsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Live countdown text derived from [VibeSettings.snoozeUntilTimestamp].
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
        }
        refreshNodes()
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
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Re-queries the Wearable Node Client for currently connected watches. */
    fun refreshNodes() {
        viewModelScope.launch {
            _nodes.value = syncManager.getConnectedNodes()
        }
    }

    fun updateIsEnabled(enabled: Boolean) = updateAndSync(_settings.value.copy(isEnabled = enabled))

    fun updateFreqMin(minutes: Int) {
        val clamped = minutes.coerceAtMost(_settings.value.freqMaxMinutes)
        updateAndSync(_settings.value.copy(freqMinMinutes = clamped))
    }

    fun updateFreqMax(minutes: Int) {
        val clamped = minutes.coerceAtLeast(_settings.value.freqMinMinutes)
        updateAndSync(_settings.value.copy(freqMaxMinutes = clamped))
    }

    fun updateIntensity(intensity: Int) =
        updateAndSync(_settings.value.copy(vibrationIntensity = intensity))

    fun updateVibrationDurationMs(ms: Long) =
        updateAndSync(_settings.value.copy(vibrationDurationMs = ms))

    fun updateVibrationPatternType(type: String) =
        updateAndSync(_settings.value.copy(vibrationPatternType = type))

    fun updateVibrationRepeatCount(count: Int) =
        updateAndSync(_settings.value.copy(vibrationRepeatCount = count))

    /**
     * Sends an immediate vibrate command to the target node(s) via MessageClient.
     * Uses the currently configured intensity and pattern settings.
     */
    fun vibrateNow() {
        val targetNodeId = _settings.value.targetNodeId
        Log.d(TAG, "vibrateNow: called — targetNodeId='$targetNodeId'")
        viewModelScope.launch {
            if (targetNodeId == VibeConstants.VALUE_TARGET_NODE_ALL) {
                Log.d(TAG, "vibrateNow: target is ALL — querying connected nodes…")
                val nodes = syncManager.getConnectedNodes()
                Log.d(TAG, "vibrateNow: found ${nodes.size} node(s) to send vibrate-now")
                nodes.forEach { node ->
                    Log.d(TAG, "vibrateNow: sending to node id='${node.id}', displayName='${node.displayName}'")
                    syncManager.sendVibrateNow(node.id)
                }
            } else {
                Log.d(TAG, "vibrateNow: sending to specific node '$targetNodeId'")
                syncManager.sendVibrateNow(targetNodeId)
            }
        }
    }

    /**
     * Sets [VibeSettings.snoozeUntilTimestamp] to [System.currentTimeMillis] + [durationMs].
     */
    fun snooze(durationMs: Long) {
        val until = System.currentTimeMillis() + durationMs
        updateAndSync(_settings.value.copy(snoozeUntilTimestamp = until))
    }

    /** Cancels any active snooze by resetting [VibeSettings.snoozeUntilTimestamp] to 0. */
    fun cancelSnooze() {
        updateAndSync(_settings.value.copy(snoozeUntilTimestamp = 0L))
    }

    /** Adds a custom snooze duration (in ms) if not already present. */
    fun addCustomSnoozeDuration(durationMs: Long) {
        val current = _settings.value.customSnoozeDurations
        if (durationMs !in current) {
            updateAndSync(_settings.value.copy(customSnoozeDurations = current + durationMs))
        }
    }

    /** Removes a custom snooze duration (in ms). */
    fun removeCustomSnoozeDuration(durationMs: Long) {
        val updated = _settings.value.customSnoozeDurations.filter { it != durationMs }
        updateAndSync(_settings.value.copy(customSnoozeDurations = updated))
    }

    /**
     * Sets the active watch by Node ID.
     * Pass [com.example.vild.shared.VibeConstants.VALUE_TARGET_NODE_ALL] to target all nodes.
     */
    fun updateTargetNode(nodeId: String) =
        updateAndSync(_settings.value.copy(targetNodeId = nodeId))

    // ── Day/Night mode API ───────────────────────────────────────────────────

    /**
     * Toggles between Day and Night mode:
     * 1. Saves current settings under the outgoing mode.
     * 2. Switches [activeMode] to the other mode.
     * 3. Loads the incoming mode's settings.
     * 4. Updates [_settings] and syncs to the watch.
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

            // Load the incoming mode's settings and sync
            val incomingSettings = repo.loadModeSettings(incoming)
            updateAndSync(incomingSettings)

            // Randomize advice for the incoming mode
            randomizeAdvice(incoming)
        }
    }

    // ── Preset API ───────────────────────────────────────────────────────────

    /** Saves the current vibration/scheduling settings as a named preset. */
    fun saveCurrentAsPreset(name: String) {
        val s = _settings.value
        val preset = Preset(
            name = name,
            isEnabled = s.isEnabled,
            freqMinMinutes = s.freqMinMinutes,
            freqMaxMinutes = s.freqMaxMinutes,
            vibrationIntensity = s.vibrationIntensity,
            vibrationDurationMs = s.vibrationDurationMs,
            vibrationPatternType = s.vibrationPatternType,
            vibrationRepeatCount = s.vibrationRepeatCount,
        )
        viewModelScope.launch { repo.savePreset(preset) }
    }

    /**
     * Applies a preset's settings to the currently active mode and syncs to the watch.
     * The loaded settings are also persisted under the active mode's DataStore key so
     * toggling away and back restores the preset values.
     */
    fun loadPreset(preset: Preset) {
        val newSettings = _settings.value.copy(
            isEnabled = preset.isEnabled,
            freqMinMinutes = preset.freqMinMinutes,
            freqMaxMinutes = preset.freqMaxMinutes,
            vibrationIntensity = preset.vibrationIntensity,
            vibrationDurationMs = preset.vibrationDurationMs,
            vibrationPatternType = preset.vibrationPatternType,
            vibrationRepeatCount = preset.vibrationRepeatCount,
        )
        updateAndSync(newSettings)
        // Also persist under the active mode so toggling away and back restores it
        viewModelScope.launch {
            repo.saveModeSettings(_activeMode.value, newSettings)
        }
    }

    /** Deletes a preset by name. */
    fun deletePreset(name: String) {
        viewModelScope.launch { repo.deletePreset(name) }
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

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun updateAndSync(newSettings: VibeSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            repo.save(newSettings)
            val success = syncManager.pushSettings(newSettings)
            _syncStatus.value = SyncStatus(
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncSuccess = success,
            )
        }
    }
}
