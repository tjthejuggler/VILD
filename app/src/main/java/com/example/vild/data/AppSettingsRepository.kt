package com.example.vild.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.vild.shared.VibeConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_vibe_settings")

/**
 * Persists vibe settings locally on the phone using DataStore Preferences.
 * Exposes a [Flow] of [VibeSettings] so the UI always reflects the latest saved state.
 */
class AppSettingsRepository(private val context: Context) {

    // ── Preference keys ──────────────────────────────────────────────────────

    private val keyIsEnabled = booleanPreferencesKey(VibeConstants.KEY_IS_ENABLED)
    private val keyFreqMin = intPreferencesKey(VibeConstants.KEY_FREQ_MIN_MINUTES)
    private val keyFreqMax = intPreferencesKey(VibeConstants.KEY_FREQ_MAX_MINUTES)
    private val keyIntensity = intPreferencesKey(VibeConstants.KEY_VIBRATION_INTENSITY)
    private val keySnoozeUntil = longPreferencesKey(VibeConstants.KEY_SNOOZE_UNTIL_TIMESTAMP)
    private val keyTargetNodeId = stringPreferencesKey(VibeConstants.KEY_TARGET_NODE_ID)

    // ── Read ─────────────────────────────────────────────────────────────────

    val settingsFlow: Flow<VibeSettings> = context.dataStore.data.map { prefs ->
        VibeSettings(
            isEnabled = prefs[keyIsEnabled] ?: false,
            freqMinMinutes = prefs[keyFreqMin] ?: 30,
            freqMaxMinutes = prefs[keyFreqMax] ?: 60,
            vibrationIntensity = prefs[keyIntensity] ?: 128,
            snoozeUntilTimestamp = prefs[keySnoozeUntil] ?: 0L,
            targetNodeId = prefs[keyTargetNodeId] ?: VibeConstants.VALUE_TARGET_NODE_ALL,
        )
    }

    // ── Write ────────────────────────────────────────────────────────────────

    suspend fun save(settings: VibeSettings) {
        context.dataStore.edit { prefs ->
            prefs[keyIsEnabled] = settings.isEnabled
            prefs[keyFreqMin] = settings.freqMinMinutes
            prefs[keyFreqMax] = settings.freqMaxMinutes
            prefs[keyIntensity] = settings.vibrationIntensity
            prefs[keySnoozeUntil] = settings.snoozeUntilTimestamp
            prefs[keyTargetNodeId] = settings.targetNodeId
        }
    }
}

/**
 * Immutable snapshot of all vibe settings stored on the phone.
 */
data class VibeSettings(
    val isEnabled: Boolean = false,
    val freqMinMinutes: Int = 30,
    val freqMaxMinutes: Int = 60,
    val vibrationIntensity: Int = 128,
    val snoozeUntilTimestamp: Long = 0L,
    val targetNodeId: String = VibeConstants.VALUE_TARGET_NODE_ALL,
)
