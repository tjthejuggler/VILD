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

private val Context.adviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "advice_store")

/**
 * Persists user-entered advice items in DataStore as JSON.
 * Mirrors the wags AdviceRepository API but uses DataStore instead of Room.
 */
class AdviceRepository(private val context: Context) {

    private val keyAdvice = stringPreferencesKey("advice_json")

    /** Observe all advice items (reactive). Deduplicates IDs on read. */
    val allAdviceFlow: Flow<List<AdviceItem>> = context.adviceDataStore.data.map { prefs ->
        loadAll(prefs)
    }

    /** Observe advice for a specific section. */
    fun observeBySection(section: String): Flow<List<AdviceItem>> =
        allAdviceFlow.map { list -> list.filter { it.section == section }.sortedByDescending { it.createdAt } }

    /** Add a new advice item. */
    suspend fun add(section: String, text: String) {
        context.adviceDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val item = AdviceItem(section = section, text = text.trim())
            prefs[keyAdvice] = Json.encodeToString(current + item)
        }
    }

    /** Update an existing advice item's text. */
    suspend fun update(item: AdviceItem, newText: String) {
        context.adviceDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val updated = current.map { if (it.id == item.id) it.copy(text = newText.trim()) else it }
            prefs[keyAdvice] = Json.encodeToString(updated)
        }
    }

    /** Delete an advice item by id. */
    suspend fun delete(id: Long) {
        context.adviceDataStore.edit { prefs ->
            val current = loadAll(prefs)
            prefs[keyAdvice] = Json.encodeToString(current.filter { it.id != id })
        }
    }

    /**
     * Loads all advice items and fixes any duplicate IDs that may exist from
     * earlier versions where `System.currentTimeMillis()` could collide.
     */
    private fun loadAll(prefs: Preferences): List<AdviceItem> {
        val json = prefs[keyAdvice] ?: return emptyList()
        val raw = runCatching { Json.decodeFromString<List<AdviceItem>>(json) }.getOrDefault(emptyList())
        // Deduplicate IDs — reassign new unique IDs to any collisions
        val seen = mutableSetOf<Long>()
        return raw.map { item ->
            if (seen.add(item.id)) item
            else item.copy(id = nextId.getAndIncrement())
        }
    }
}
