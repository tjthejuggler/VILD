package com.example.vild.wear

import android.content.Context
import android.content.SharedPreferences
import com.example.vild.shared.VibeConstants

/**
 * Local storage for vibe settings using SharedPreferences.
 * Acts as the single source of truth on the watch side.
 */
object VibeSettingsRepository {

    private const val PREFS_NAME = "vibe_settings_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        isEnabled: Boolean,
        freqMinMinutes: Int,
        freqMaxMinutes: Int,
        vibrationIntensity: Int,
        snoozeUntilTimestamp: Long,
        targetNodeId: String,
    ) {
        prefs(context).edit()
            .putBoolean(VibeConstants.KEY_IS_ENABLED, isEnabled)
            .putInt(VibeConstants.KEY_FREQ_MIN_MINUTES, freqMinMinutes)
            .putInt(VibeConstants.KEY_FREQ_MAX_MINUTES, freqMaxMinutes)
            .putInt(VibeConstants.KEY_VIBRATION_INTENSITY, vibrationIntensity)
            .putLong(VibeConstants.KEY_SNOOZE_UNTIL_TIMESTAMP, snoozeUntilTimestamp)
            .putString(VibeConstants.KEY_TARGET_NODE_ID, targetNodeId)
            .apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(VibeConstants.KEY_IS_ENABLED, false)

    fun freqMinMinutes(context: Context): Int =
        prefs(context).getInt(VibeConstants.KEY_FREQ_MIN_MINUTES, 30)

    fun freqMaxMinutes(context: Context): Int =
        prefs(context).getInt(VibeConstants.KEY_FREQ_MAX_MINUTES, 60)

    fun vibrationIntensity(context: Context): Int =
        prefs(context).getInt(VibeConstants.KEY_VIBRATION_INTENSITY, 128)

    fun snoozeUntilTimestamp(context: Context): Long =
        prefs(context).getLong(VibeConstants.KEY_SNOOZE_UNTIL_TIMESTAMP, 0L)

    fun targetNodeId(context: Context): String =
        prefs(context).getString(VibeConstants.KEY_TARGET_NODE_ID, VibeConstants.VALUE_TARGET_NODE_ALL)
            ?: VibeConstants.VALUE_TARGET_NODE_ALL
}
