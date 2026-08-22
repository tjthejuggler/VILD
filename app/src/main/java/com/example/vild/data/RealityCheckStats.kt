package com.example.vild.data

/**
 * Pure, testable statistics derived from a list of [RealityCheckDayLog]s.
 * No Android dependencies — computed on demand by the UI layer.
 */
data class RealityCheckStats(
    /** Consecutive days (ending today, or yesterday if today isn't read yet) with readAt set. */
    val currentReadStreak: Int = 0,
    /** Consecutive days with doneAt set. */
    val currentDoneStreak: Int = 0,
    /** Longest-ever run of consecutive read days. */
    val bestReadStreak: Int = 0,
    /** Longest-ever run of consecutive done days. */
    val bestDoneStreak: Int = 0,
    /** Total days a reality check was chosen. */
    val totalDays: Int = 0,
    /** Total days confirmed read. */
    val totalRead: Int = 0,
    /** Total days confirmed done. */
    val totalDone: Int = 0,
    /** Per-trigger leaderboard, sorted by times done (desc). */
    val perTrigger: List<TriggerStat> = emptyList(),
)

/** Aggregate counts for a single trigger text. */
data class TriggerStat(
    val text: String,
    val timesShown: Int,
    val timesRead: Int,
    val timesDone: Int,
)

/** Computes [RealityCheckStats] from day logs (any order; duplicates ignored). */
fun computeStats(logs: List<RealityCheckDayLog>): RealityCheckStats {
    val unique = logs.distinctBy { it.epochDay }
    val readDays = unique.filter { it.readAt != null }.map { it.epochDay }.toSet()
    val doneDays = unique.filter { it.doneAt != null }.map { it.epochDay }.toSet()

    val today = todayEpochDay()

    return RealityCheckStats(
        currentReadStreak = currentStreak(readDays, today),
        currentDoneStreak = currentStreak(doneDays, today),
        bestReadStreak = bestStreak(readDays),
        bestDoneStreak = bestStreak(doneDays),
        totalDays = unique.size,
        totalRead = readDays.size,
        totalDone = doneDays.size,
        perTrigger = unique
            .groupBy { it.triggerText }
            .map { (text, dayLogs) ->
                TriggerStat(
                    text = text,
                    timesShown = dayLogs.size,
                    timesRead = dayLogs.count { it.readAt != null },
                    timesDone = dayLogs.count { it.doneAt != null },
                )
            }
            .sortedWith(compareByDescending<TriggerStat> { it.timesDone }.thenByDescending { it.timesRead }),
    )
}

/**
 * Current streak ending today; if today isn't in the set, the streak is
 * measured up to yesterday (the day isn't over yet — grace until midnight).
 */
private fun currentStreak(days: Set<Long>, today: Long): Int {
    var start = today
    if (start !in days) start-- // allow "yesterday" to still carry the streak
    if (start !in days) return 0
    var streak = 0
    var day = start
    while (day in days) {
        streak++
        day--
    }
    return streak
}

/** Longest run of consecutive days anywhere in the set. */
private fun bestStreak(days: Set<Long>): Int {
    if (days.isEmpty()) return 0
    var best = 1
    for (day in days) {
        if (day - 1 in days) continue // not the start of a run
        var run = 1
        var d = day
        while (d + 1 in days) {
            run++
            d++
        }
        if (run > best) best = run
    }
    return best
}
