package com.example.vild

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vild.data.AppSettingsRepository
import com.example.vild.data.VibeSettings
import com.example.vild.data.WearSyncManager
import com.google.android.gms.wearable.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    /**
     * Sets [VibeSettings.snoozeUntilTimestamp] to [System.currentTimeMillis] + [durationMs].
     */
    fun snooze(durationMs: Long) {
        val until = System.currentTimeMillis() + durationMs
        updateAndSync(_settings.value.copy(snoozeUntilTimestamp = until))
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
