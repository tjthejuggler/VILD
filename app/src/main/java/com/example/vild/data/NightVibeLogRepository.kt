package com.example.vild.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

private val Context.nightVibeLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "night_vibe_log")

/**
 * Records every night-vibe notification actually sent ([NightVibeEntry]),
 * including the user's after-the-fact annotations (noticed / in-dream / woke
 * me up). Entries older than [MAX_AGE_DAYS] days are pruned on write.
 */
class NightVibeLogRepository(private val context: Context) {

    private val keyEntries = stringPreferencesKey("sent_times_json")
    private val keyPollutionPurgeDone = booleanPreferencesKey("pollution_purge_done_v1")
    private val keyLastBedtime = longPreferencesKey("last_bedtime_ms")

    /** Observe all recorded entries, oldest → newest. */
    val entriesFlow: Flow<List<NightVibeEntry>> = context.nightVibeLogDataStore.data.map { prefs ->
        loadAll(prefs)
    }

    /** Appends a freshly-sent pulse and prunes entries older than the retention window. */
    suspend fun record(timestampMs: Long) {
        context.nightVibeLogDataStore.edit { prefs ->
            val cutoff = timestampMs - MAX_AGE_DAYS * 24 * 60 * 60_000L
            val updated = (loadAll(prefs) + NightVibeEntry(timestampMs))
                .filter { it.timestampMs >= cutoff }
                .sortedBy { it.timestampMs }
            prefs[keyEntries] = Json.encodeToString(updated)
        }
    }

    /** Replaces an entry (identified by its timestamp) with [updated]. */
    suspend fun updateEntry(updated: NightVibeEntry) {
        context.nightVibeLogDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val replaced = current.map {
                if (it.timestampMs == updated.timestampMs) updated else it
            }
            prefs[keyEntries] = Json.encodeToString(replaced)
        }
    }

    /** Epoch-ms of the most recent Goodnight confirmation; 0 if none recorded. */
    val lastBedtimeFlow: Flow<Long> = context.nightVibeLogDataStore.data.map { prefs ->
        prefs[keyLastBedtime] ?: 0L
    }

    /**
     * Stamps the moment the user tapped **Goodnight** as the bedtime anchor
     * the whole night schedule is measured from.
     */
    suspend fun recordBedtime(timestampMs: Long) {
        context.nightVibeLogDataStore.edit { prefs ->
            prefs[keyLastBedtime] = timestampMs
        }
    }

    /**
     * One-time cleanup for the 2026-09-08/09 scheduler bug that fired vibes all
     * day long, flooding the log with daytime pulses. Deletes every entry from
     * those two local dates and sets a flag so the purge never runs twice.
     */
    suspend fun purgePollutedDaysOnce() {
        context.nightVibeLogDataStore.edit { prefs ->
            if (prefs[keyPollutionPurgeDone] == true) return@edit
            val zone = ZoneId.systemDefault()
            val from = LocalDate.of(2026, 9, 8).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = LocalDate.of(2026, 9, 10).atStartOfDay(zone).toInstant().toEpochMilli()
            val kept = loadAll(prefs).filter { it.timestampMs < from || it.timestampMs >= to }
            prefs[keyEntries] = Json.encodeToString(kept)
            prefs[keyPollutionPurgeDone] = true
        }
    }

    private fun loadAll(prefs: Preferences): List<NightVibeEntry> {
        val json = prefs[keyEntries] ?: return emptyList()
        return runCatching { Json.decodeFromString<List<NightVibeEntry>>(json) }
            .getOrElse { emptyList() }
    }

    private companion object {
        const val MAX_AGE_DAYS = 14L
    }
}
