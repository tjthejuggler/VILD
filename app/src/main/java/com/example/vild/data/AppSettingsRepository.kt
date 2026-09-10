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
    private val keyBedtimePrompt = intPreferencesKey("bedtime_prompt_minutes")
    private val keyBedtimeAnchor = longPreferencesKey("bedtime_anchor_ms")
    private val keyAdaptiveInterval = booleanPreferencesKey("adaptive_interval")
    private val keyGapMinutes = intPreferencesKey("gap_minutes")
    private val keyNightEnd = intPreferencesKey("night_end_minutes")
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
            bedtimePromptMinutes = prefs[keyBedtimePrompt] ?: 22 * 60 + 30,
            bedtimeAnchorMs = prefs[keyBedtimeAnchor] ?: 0L,
            adaptiveInterval = prefs[keyAdaptiveInterval] ?: true,
            gapMinutes = prefs[keyGapMinutes] ?: 240,
            nightEndMinutes = prefs[keyNightEnd] ?: 480,
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
            prefs[keyBedtimePrompt] = settings.bedtimePromptMinutes
            prefs[keyBedtimeAnchor] = settings.bedtimeAnchorMs
            prefs[keyAdaptiveInterval] = settings.adaptiveInterval
            prefs[keyGapMinutes] = settings.gapMinutes
            prefs[keyNightEnd] = settings.nightEndMinutes
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
 * Scheduling is anchored to the user's real bedtime: at [bedtimePromptMinutes]
 * the app asks "Going to sleep?" and tapping **Goodnight** stamps
 * [bedtimeAnchorMs]. The quiet gap and every REM-targeted pulse are measured
 * from that anchor — nothing fires at fixed clock times anymore.
 *
 * @property isEnabled             Whether night vibes are armed at all.
 * @property nightStartMinutes     LEGACY (pre-anchor) night start, kept only so
 *                                 old day/night JSON snapshots still parse.
 * @property bedtimePromptMinutes  Minutes-of-day when the "Going to sleep?"
 *                                 prompt fires (may wrap past midnight, e.g. 01:00 → 1500).
 * @property bedtimeAnchorMs       Epoch-ms of the last confirmed Goodnight; 0 = none yet.
 *                                 The whole night schedule is relative to this moment.
 * @property adaptiveInterval      Whether [SleepLearning] may auto-tune
 *                                 [remIntervalMinutes] from the user's pulse feedback.
 * @property gapMinutes            Silent gap after the Goodnight anchor — the first
 *                                 part of the night, when REM sleep is sparse.
 * @property nightEndMinutes       Minutes-of-day wall-clock cap for the night window
 *                                 (e.g. 08:00 → 480). No vibes after it, whenever
 *                                 the user went to bed.
 * @property remIntervalMinutes    Starting sleep-cycle length; after the gap, one
 *                                 notification per cycle, aimed at predicted REM.
 *                                 Auto-tuned by [SleepLearning] when [adaptiveInterval].
 * @property snoozeUntilTimestamp  Epoch-ms until which night vibes are paused.
 * @property customSnoozeDurations User-defined snooze durations (ms), phone-UI concern only.
 */
@Serializable
data class NightVibeSettings(
    val isEnabled: Boolean = false,
    val nightStartMinutes: Int = 23 * 60,
    val bedtimePromptMinutes: Int = 22 * 60 + 30,
    val bedtimeAnchorMs: Long = 0L,
    val adaptiveInterval: Boolean = true,
    val gapMinutes: Int = 240,
    /** Minutes-of-day when the night window ends (e.g. 08:00 → 480). */
    val nightEndMinutes: Int = 480,
    val remIntervalMinutes: Int = 90,
    val snoozeUntilTimestamp: Long = 0L,
    /** Stored as comma-separated string in DataStore; phone-UI concern only. */
    val customSnoozeDurations: List<Long> = emptyList(),
)
