package com.example.vild

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.VibeSettings
import com.example.vild.data.WearSyncManager
import com.example.vild.shared.VibeConstants
import com.google.android.gms.wearable.Node
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

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

    // ── UI state ─────────────────────────────────────────────────────────────

    private val _settings = MutableStateFlow(VibeSettings())
    val settings: StateFlow<VibeSettings> = _settings.asStateFlow()

    private val _nodes = MutableStateFlow<List<Node>>(emptyList())
    val nodes: StateFlow<List<Node>> = _nodes.asStateFlow()

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
            _settings.value = repo.settingsFlow.first()
        }
        refreshNodes()
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

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun updateAndSync(newSettings: VibeSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            repo.save(newSettings)
            syncManager.pushSettings(newSettings)
        }
    }
}
