package com.example.vild.shared

/**
 * Single source of truth for Wearable Data Layer paths and keys
 * shared between the mobile (`app`) and Wear OS (`wear`) modules.
 */
object VibeConstants {

    /** Data Layer path for vibe settings. */
    const val PATH_VIBE_SETTINGS = "/vibe_settings"

    // ── Keys ────────────────────────────────────────────────────────────────

    /** Boolean – whether vibration reminders are enabled. */
    const val KEY_IS_ENABLED = "is_enabled"

    /** Int – minimum interval between reminders, in minutes. */
    const val KEY_FREQ_MIN_MINUTES = "freq_min_minutes"

    /** Int – maximum interval between reminders, in minutes. */
    const val KEY_FREQ_MAX_MINUTES = "freq_max_minutes"

    /** Int – vibration motor intensity level. */
    const val KEY_VIBRATION_INTENSITY = "vibration_intensity"

    /** Long – epoch-millisecond timestamp until which reminders are snoozed. */
    const val KEY_SNOOZE_UNTIL_TIMESTAMP = "snooze_until_timestamp"

    /**
     * String – Node ID of the watch that should actively vibrate.
     * Use [VALUE_TARGET_NODE_ALL] to broadcast to all connected nodes.
     */
    const val KEY_TARGET_NODE_ID = "target_node_id"

    /** Sentinel value meaning every connected node should vibrate. */
    const val VALUE_TARGET_NODE_ALL = "all"
}
