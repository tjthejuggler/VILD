package com.example.vild.ui.advice

/**
 * String constants for the two advice sections in VILD.
 * These match the `section` field stored in [com.example.vild.data.AdviceItem].
 */
object AdviceSection {
    const val DAY = "day"
    const val NIGHT = "night"

    /** Human-readable label for each section key. */
    fun label(section: String): String = when (section) {
        DAY -> "Day"
        NIGHT -> "Night"
        else -> section
    }

    /** All sections in display order. */
    val all = listOf(DAY, NIGHT)
}
