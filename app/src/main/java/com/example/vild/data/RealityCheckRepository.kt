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

private val Context.triggerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reality_check_store")

/**
 * Persists reality check triggers in DataStore as JSON.
 * Same pattern as [AdviceRepository].
 */
class RealityCheckRepository(private val context: Context) {

    private val keyTriggers = stringPreferencesKey("triggers_json")

    /** Observe all triggers (reactive). Deduplicates IDs on read. */
    val allTriggersFlow: Flow<List<RealityCheckTrigger>> = context.triggerDataStore.data.map { prefs ->
        loadAll(prefs)
    }

    /** Add a new trigger. */
    suspend fun add(text: String) {
        context.triggerDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val item = RealityCheckTrigger(text = text.trim())
            prefs[keyTriggers] = Json.encodeToString(current + item)
        }
    }

    /** Update an existing trigger's text. */
    suspend fun update(item: RealityCheckTrigger, newText: String) {
        context.triggerDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val updated = current.map { if (it.id == item.id) it.copy(text = newText.trim()) else it }
            prefs[keyTriggers] = Json.encodeToString(updated)
        }
    }

    /** Delete a trigger by id. */
    suspend fun delete(id: Long) {
        context.triggerDataStore.edit { prefs ->
            val current = loadAll(prefs)
            prefs[keyTriggers] = Json.encodeToString(current.filter { it.id != id })
        }
    }

    /**
     * Loads all triggers and fixes any duplicate IDs that may exist from
     * earlier versions where `System.currentTimeMillis()` could collide.
     */
    private fun loadAll(prefs: Preferences): List<RealityCheckTrigger> {
        val json = prefs[keyTriggers] ?: return emptyList()
        val raw = runCatching { Json.decodeFromString<List<RealityCheckTrigger>>(json) }.getOrDefault(emptyList())
        // Deduplicate IDs — reassign new unique IDs to any collisions
        val seen = mutableSetOf<Long>()
        return raw.map { item ->
            if (seen.add(item.id)) item
            else item.copy(id = triggerNextId.getAndIncrement())
        }
    }
}
