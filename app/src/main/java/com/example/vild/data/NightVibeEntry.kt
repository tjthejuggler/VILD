package com.example.vild.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.Serializable

/**
 * One recorded night-vibe pulse: when it was sent and how the sleeper
 * experienced it (all flags default to "unmarked").
 *
 * @property timestampMs Epoch-ms when the notification was sent.
 * @property noticed     True if the user remembers noticing the pulse.
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
