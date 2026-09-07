package com.example.vild.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.nightVibeLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "night_vibe_log")

/**
 * Records every night-vibe notification actually sent ([NightVibeEntry]),
 * including the user's after-the-fact annotations (noticed / in-dream / woke
 * me up). Entries older than [MAX_AGE_DAYS] days are pruned on write.
 */
class NightVibeLogRepository(private val context: Context) {

    private val keyEntries = stringPreferencesKey("sent_times_json")

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

    private fun loadAll(prefs: Preferences): List<NightVibeEntry> {
        val json = prefs[keyEntries] ?: return emptyList()
        return runCatching { Json.decodeFromString<List<NightVibeEntry>>(json) }
            .getOrElse { emptyList() }
    }

    private companion object {
        const val MAX_AGE_DAYS = 14L
    }
}
