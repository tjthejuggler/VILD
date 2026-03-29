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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    private val keyVibrationDurationMs = longPreferencesKey(VibeConstants.KEY_VIBRATION_DURATION_MS)
    private val keyVibrationPatternType = stringPreferencesKey(VibeConstants.KEY_VIBRATION_PATTERN_TYPE)
    private val keyVibrationRepeatCount = intPreferencesKey(VibeConstants.KEY_VIBRATION_REPEAT_COUNT)
    private val keyCustomSnoozeDurations = stringPreferencesKey("custom_snooze_durations")
    private val keyPresets = stringPreferencesKey("presets_json")

    // ── Day/Night mode keys ──────────────────────────────────────────────────

    private val keyActiveMode = stringPreferencesKey("active_mode")
    private val keyDaySettings = stringPreferencesKey("day_settings_json")
    private val keyNightSettings = stringPreferencesKey("night_settings_json")

    // ── Read ─────────────────────────────────────────────────────────────────

    val settingsFlow: Flow<VibeSettings> = context.dataStore.data.map { prefs ->
        VibeSettings(
            isEnabled = prefs[keyIsEnabled] ?: false,
            freqMinMinutes = prefs[keyFreqMin] ?: 30,
            freqMaxMinutes = prefs[keyFreqMax] ?: 60,
            vibrationIntensity = prefs[keyIntensity] ?: 128,
            snoozeUntilTimestamp = prefs[keySnoozeUntil] ?: 0L,
            targetNodeId = prefs[keyTargetNodeId] ?: VibeConstants.VALUE_TARGET_NODE_ALL,
            vibrationDurationMs = prefs[keyVibrationDurationMs] ?: 500L,
            vibrationPatternType = prefs[keyVibrationPatternType] ?: "single",
            vibrationRepeatCount = prefs[keyVibrationRepeatCount] ?: 1,
            customSnoozeDurations = prefs[keyCustomSnoozeDurations]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { it.toLong() }
                ?: emptyList(),
        )
    }

    val presetsFlow: Flow<List<Preset>> = context.dataStore.data.map { prefs ->
        val json = prefs[keyPresets] ?: return@map emptyList()
        runCatching { Json.decodeFromString<List<Preset>>(json) }.getOrDefault(emptyList())
    }

    /** Emits `"day"` or `"night"` — defaults to `"day"` if never set. */
    val activeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyActiveMode] ?: "day"
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
            prefs[keyVibrationDurationMs] = settings.vibrationDurationMs
            prefs[keyVibrationPatternType] = settings.vibrationPatternType
            prefs[keyVibrationRepeatCount] = settings.vibrationRepeatCount
            prefs[keyCustomSnoozeDurations] = settings.customSnoozeDurations.joinToString(",")
        }
    }

    /** Adds or replaces a preset by name. */
    suspend fun savePreset(preset: Preset) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                Json.decodeFromString<List<Preset>>(prefs[keyPresets] ?: "[]")
            }.getOrDefault(emptyList())
            val updated = current.filter { it.name != preset.name } + preset
            prefs[keyPresets] = Json.encodeToString(updated)
        }
    }

    /** Removes a preset by name. No-op if the name does not exist. */
    suspend fun deletePreset(name: String) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                Json.decodeFromString<List<Preset>>(prefs[keyPresets] ?: "[]")
            }.getOrDefault(emptyList())
            prefs[keyPresets] = Json.encodeToString(current.filter { it.name != name })
        }
    }

    // ── Day/Night mode ───────────────────────────────────────────────────────

    /** Persists [settings] under the given [mode] key (`"day"` or `"night"`). */
    suspend fun saveModeSettings(mode: String, settings: VibeSettings) {
        val key = if (mode == "night") keyNightSettings else keyDaySettings
        val json = Json.encodeToString(settings)
        context.dataStore.edit { prefs -> prefs[key] = json }
    }

    /**
     * Loads the [VibeSettings] stored for [mode] (`"day"` or `"night"`).
     * Falls back to the current active settings if no mode snapshot exists yet.
     */
    suspend fun loadModeSettings(mode: String): VibeSettings {
        val key = if (mode == "night") keyNightSettings else keyDaySettings
        val prefs = context.dataStore.data.first()
        val json = prefs[key] ?: return settingsFlow.first()
        return runCatching { Json.decodeFromString<VibeSettings>(json) }
            .getOrElse { settingsFlow.first() }
    }

    /** Persists the active mode (`"day"` or `"night"`) to DataStore. */
    suspend fun setActiveMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[keyActiveMode] = mode }
    }
}

/**
 * Immutable snapshot of all vibe settings stored on the phone.
 *
 * Annotated with [@Serializable] so it can be stored as JSON for Day/Night mode snapshots.
 */
@Serializable
data class VibeSettings(
    val isEnabled: Boolean = false,
    val freqMinMinutes: Int = 30,
    val freqMaxMinutes: Int = 60,
    val vibrationIntensity: Int = 128,
    val snoozeUntilTimestamp: Long = 0L,
    val targetNodeId: String = VibeConstants.VALUE_TARGET_NODE_ALL,
    val vibrationDurationMs: Long = 500L,
    val vibrationPatternType: String = "single",
    val vibrationRepeatCount: Int = 1,
    /** Stored as comma-separated string in DataStore; phone-UI concern only. */
    val customSnoozeDurations: List<Long> = emptyList(),
)
