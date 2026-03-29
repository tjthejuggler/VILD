package com.example.vild.data

import kotlinx.serialization.Serializable

/**
 * A named snapshot of vibration/scheduling settings.
 *
 * Intentionally excludes transient/device-specific fields:
 * [VibeSettings.snoozeUntilTimestamp], [VibeSettings.targetNodeId],
 * and [VibeSettings.customSnoozeDurations].
 *
 * Stored as part of a JSON array under the DataStore key `presets_json`.
 */
@Serializable
data class Preset(
    val name: String,
    val isEnabled: Boolean = false,
    val freqMinMinutes: Int = 30,
    val freqMaxMinutes: Int = 60,
    val vibrationIntensity: Int = 128,
    val vibrationDurationMs: Long = 500L,
    val vibrationPatternType: String = "single",
    val vibrationRepeatCount: Int = 1,
)
