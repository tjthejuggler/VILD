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
 * Records the epoch-ms timestamps of every night-vibe notification actually
 * sent, so the app can show "at what time notifications were sent throughout
 * the night". Entries older than [MAX_AGE_DAYS] days are pruned on write.
 */
class NightVibeLogRepository(private val context: Context) {

    private val keySentTimes = stringPreferencesKey("sent_times_json")

    /** Observe all recorded send timestamps, oldest → newest. */
    val sentTimesFlow: Flow<List<Long>> = context.nightVibeLogDataStore.data.map { prefs ->
        loadAll(prefs)
    }

    /** Appends [timestampMs] and prunes entries older than the retention window. */
    suspend fun record(timestampMs: Long) {
        context.nightVibeLogDataStore.edit { prefs ->
            val cutoff = timestampMs - MAX_AGE_DAYS * 24 * 60 * 60_000L
            val updated = (loadAll(prefs) + timestampMs)
                .filter { it >= cutoff }
                .sorted()
            prefs[keySentTimes] = Json.encodeToString(updated)
        }
    }

    private fun loadAll(prefs: Preferences): List<Long> {
        val json = prefs[keySentTimes] ?: return emptyList()
        return runCatching { Json.decodeFromString<List<Long>>(json) }.getOrDefault(emptyList())
    }

    private companion object {
        const val MAX_AGE_DAYS = 14L
    }
}
