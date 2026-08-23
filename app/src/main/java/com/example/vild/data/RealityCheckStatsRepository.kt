package com.example.vild.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dayLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "reality_check_day_logs")

/**
 * Persists one [RealityCheckDayLog] per day in DataStore as JSON.
 * Same pattern as [RealityCheckRepository].
 *
 * The log for the current day is the app's primary object: it is shown on the
 * main screen the moment the app opens, and the nagging notification keeps
 * referencing it until [RealityCheckDayLog.isComplete].
 */
class RealityCheckStatsRepository(private val context: Context) {

    private val keyLogs = stringPreferencesKey("day_logs_json")

    /** Observe all day logs (reactive), sorted oldest → newest. */
    val allLogsFlow: Flow<List<RealityCheckDayLog>> = context.dayLogDataStore.data.map { prefs ->
        loadAll(prefs).sortedBy { it.epochDay }
    }

    /**
     * Returns today's log, creating it first (with a random trigger) if needed.
     * If no triggers exist, the [FALLBACK_TRIGGER_TEXT] is used so the daily
     * practice still works before the user configures anything.
     */
    suspend fun ensureTodayLog(triggers: List<RealityCheckTrigger>): RealityCheckDayLog {
        val today = todayEpochDay()
        context.dayLogDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val existing = current.firstOrNull { it.epochDay == today }
            if (existing == null) {
                val trigger = triggers.randomOrNull()
                val log = RealityCheckDayLog(
                    epochDay = today,
                    triggerId = trigger?.id ?: 0L,
                    triggerText = trigger?.text ?: FALLBACK_TRIGGER_TEXT,
                )
                prefs[keyLogs] = Json.encodeToString(current + log)
            }
        }
        // Return the canonical stored value.
        return loadAll(context.dayLogDataStore.data.first()).first { it.epochDay == today }
    }

    /**
     * Records one more "I read it" round for today. The button is repeatable:
     * the first call sets [RealityCheckDayLog.readAt] and every call —
     * including later ones — bumps [RealityCheckDayLog.readCount].
     */
    suspend fun markReadToday(): RealityCheckDayLog? = updateToday {
        val now = System.currentTimeMillis()
        it.copy(
            readAt = it.readAt ?: now,
            readCount = it.readCount + 1,
        )
    }

    /**
     * Records one more "I did it" round for today. The button is repeatable:
     * the first call sets [RealityCheckDayLog.doneAt] (completing the day) and
     * every call — including later ones — bumps [RealityCheckDayLog.doneCount].
     */
    suspend fun markDoneToday(): RealityCheckDayLog? = updateToday {
        val now = System.currentTimeMillis()
        it.copy(
            doneAt = it.doneAt ?: now,
            doneCount = it.doneCount + 1,
        )
    }

    private suspend fun updateToday(transform: (RealityCheckDayLog) -> RealityCheckDayLog): RealityCheckDayLog? {
        val today = todayEpochDay()
        var updated: RealityCheckDayLog? = null
        context.dayLogDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val todayLog = current.firstOrNull { it.epochDay == today } ?: return@edit
            val new = transform(todayLog)
            updated = new
            prefs[keyLogs] = Json.encodeToString(current.map { if (it.epochDay == today) new else it })
        }
        return updated
    }

    private fun loadAll(prefs: Preferences): List<RealityCheckDayLog> {
        val json = prefs[keyLogs] ?: return emptyList()
        return runCatching { Json.decodeFromString<List<RealityCheckDayLog>>(json) }.getOrDefault(emptyList())
    }
}
