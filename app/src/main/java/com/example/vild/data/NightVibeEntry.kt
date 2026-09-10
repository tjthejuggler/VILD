package com.example.vild.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.Serializable

/**
 * One recorded night-vibe pulse: when it was sent and how the sleeper
 * experienced it (all flags default to "unmarked" = unnoticed).
 *
 * The experience is a radio choice — exactly one of [NightVibeMark.UNNOTICED],
 * [NightVibeMark.IN_DREAM] or [NightVibeMark.WOKE_ME] per pulse; see
 * [mark]/[withMark].
 *
 * @property timestampMs Epoch-ms when the notification was sent.
 * @property noticed     True if the pulse entered the sleeper's awareness
 *                       (set together with [inDream] or [wokeMeUp]).
 * @property inDream     True if the pulse was noticed inside a dream.
 * @property wokeMeUp    True if the pulse woke the user up.
 */
@Serializable
data class NightVibeEntry(
    val timestampMs: Long,
    val noticed: Boolean = false,
    val inDream: Boolean = false,
    val wokeMeUp: Boolean = false,
)

/**
 * How the sleeper experienced one night-vibe pulse — exactly one per entry.
 */
enum class NightVibeMark { UNNOTICED, IN_DREAM, WOKE_ME }

/** The pulse's current marker (legacy noticed-only entries read as in-dream). */
val NightVibeEntry.mark: NightVibeMark
    get() = when {
        wokeMeUp -> NightVibeMark.WOKE_ME
        inDream || noticed -> NightVibeMark.IN_DREAM
        else -> NightVibeMark.UNNOTICED
    }

/** Returns a copy of [entry] with exactly one marker set (radio semantics). */
fun NightVibeEntry.withMark(mark: NightVibeMark): NightVibeEntry = when (mark) {
    NightVibeMark.UNNOTICED -> copy(noticed = false, inDream = false, wokeMeUp = false)
    NightVibeMark.IN_DREAM -> copy(noticed = true, inDream = true, wokeMeUp = false)
    NightVibeMark.WOKE_ME -> copy(noticed = true, inDream = false, wokeMeUp = true)
}

/**
 * Groups entries by the calendar date their night started on: a send time
 * before the night-start time-of-day belongs to the previous date's night
 * (handles the midnight wrap, e.g. start 23:00 → pulses at 02:00 belong to
 * yesterday's night).
 */
fun groupNights(
    entries: List<NightVibeEntry>,
    nightStartMinutes: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, List<NightVibeEntry>> = entries
    .groupBy { entry ->
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.timestampMs), zone)
        var nightDate = time.toLocalDate()
        if (time.toLocalTime() < LocalTime.of(nightStartMinutes / 60, nightStartMinutes % 60)) {
            nightDate = nightDate.minusDays(1)
        }
        nightDate
    }
    .toSortedMap(compareByDescending { it })
