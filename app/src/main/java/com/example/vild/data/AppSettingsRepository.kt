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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_vibe_settings")

/**
 * Persists night-vibe settings locally on the phone using DataStore Preferences.
 * Exposes a [Flow] of [NightVibeSettings] so the UI always reflects the latest saved state.
 */
class AppSettingsRepository(private val context: Context) {

    // ── Preference keys ──────────────────────────────────────────────────────

    private val keyIsEnabled = booleanPreferencesKey("is_enabled")
    private val keyNightStart = intPreferencesKey("night_start_minutes")
    private val keyGapMinutes = intPreferencesKey("gap_minutes")
    private val keyRemInterval = intPreferencesKey("rem_interval_minutes")
    private val keySnoozeUntil = longPreferencesKey("snooze_until_timestamp")
    private val keyCustomSnoozeDurations = stringPreferencesKey("custom_snooze_durations")

    // ── Day/Night mode keys ──────────────────────────────────────────────────

    private val keyActiveMode = stringPreferencesKey("active_mode")
    private val keyDaySettings = stringPreferencesKey("day_settings_json")
    private val keyNightSettings = stringPreferencesKey("night_settings_json")

    // ── Tail integration keys ────────────────────────────────────────────────

    private val keyAutoSwitchDayOnHabit = booleanPreferencesKey("auto_switch_day_on_habit")

    // ── Read ─────────────────────────────────────────────────────────────────

    val settingsFlow: Flow<NightVibeSettings> = context.dataStore.data.map { prefs ->
        NightVibeSettings(
            isEnabled = prefs[keyIsEnabled] ?: false,
            nightStartMinutes = prefs[keyNightStart] ?: 23 * 60,
            gapMinutes = prefs[keyGapMinutes] ?: 240,
            remIntervalMinutes = prefs[keyRemInterval] ?: 90,
            snoozeUntilTimestamp = prefs[keySnoozeUntil] ?: 0L,
            customSnoozeDurations = prefs[keyCustomSnoozeDurations]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.map { it.toLong() }
                ?: emptyList(),
        )
    }

    /** Emits `"day"` or `"night"` — defaults to `"day"` if never set. */
    val activeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyActiveMode] ?: "day"
    }

    /** Whether to auto-switch from night → day when Tail reports a habit increment. */
    val autoSwitchDayOnHabitFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyAutoSwitchDayOnHabit] ?: false
    }

    // ── Write ────────────────────────────────────────────────────────────────

    suspend fun save(settings: NightVibeSettings) {
        context.dataStore.edit { prefs ->
            prefs[keyIsEnabled] = settings.isEnabled
            prefs[keyNightStart] = settings.nightStartMinutes
            prefs[keyGapMinutes] = settings.gapMinutes
            prefs[keyRemInterval] = settings.remIntervalMinutes
            prefs[keySnoozeUntil] = settings.snoozeUntilTimestamp
            prefs[keyCustomSnoozeDurations] = settings.customSnoozeDurations.joinToString(",")
        }
    }

    // ── Day/Night mode ───────────────────────────────────────────────────────

    /** Persists [settings] under the given [mode] key (`"day"` or `"night"`). */
    suspend fun saveModeSettings(mode: String, settings: NightVibeSettings) {
        val key = if (mode == "night") keyNightSettings else keyDaySettings
        val json = Json.encodeToString(settings)
        context.dataStore.edit { prefs -> prefs[key] = json }
    }

    /**
     * Loads the [NightVibeSettings] stored for [mode] (`"day"` or `"night"`).
     * Falls back to the current active settings if no mode snapshot exists yet.
     */
    suspend fun loadModeSettings(mode: String): NightVibeSettings {
        val key = if (mode == "night") keyNightSettings else keyDaySettings
        val prefs = context.dataStore.data.first()
        val json = prefs[key] ?: return settingsFlow.first()
        return runCatching { Json.decodeFromString<NightVibeSettings>(json) }
            .getOrElse { settingsFlow.first() }
    }

    /** Persists the active mode (`"day"` or `"night"`) to DataStore. */
    suspend fun setActiveMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[keyActiveMode] = mode }
    }

    /** Persists the auto-switch-day-on-habit toggle. */
    suspend fun setAutoSwitchDayOnHabit(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[keyAutoSwitchDayOnHabit] = enabled }
    }
}

/**
 * Immutable snapshot of the night-vibe settings stored on the phone.
 *
 * The phone posts plain notifications during the night; a paired wearable
 * (e.g. a Garmin watch) mirrors them and vibrates — no wearable-specific code.
 *
 * @property isEnabled           Whether night vibes are armed at all.
 * @property nightStartMinutes    Minutes-of-day when the night window begins (e.g. 23:00 → 1380).
 * @property gapMinutes           Silent gap after [nightStartMinutes] — the first half of the
 *                                night, when REM sleep is sparse. No notifications are sent.
 * @property remIntervalMinutes   Estimated sleep-cycle length; after the gap, one notification
 *                                is sent per cycle (aimed at predicted REM sessions).
 *                                The night has no fixed end time — it ends when the app
 *                                leaves night mode (Tail habit increment or manual toggle).
 * @property snoozeUntilTimestamp Epoch-ms until which night vibes are paused.
 * @property customSnoozeDurations User-defined snooze durations (ms), phone-UI concern only.
 */
@Serializable
data class NightVibeSettings(
    val isEnabled: Boolean = false,
    val nightStartMinutes: Int = 23 * 60,
    val gapMinutes: Int = 240,
    val remIntervalMinutes: Int = 90,
    val snoozeUntilTimestamp: Long = 0L,
    /** Stored as comma-separated string in DataStore; phone-UI concern only. */
    val customSnoozeDurations: List<Long> = emptyList(),
)
