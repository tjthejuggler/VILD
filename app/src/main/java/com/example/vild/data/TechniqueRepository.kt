package com.example.vild.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.techniqueDataStore: DataStore<Preferences> by preferencesDataStore(name = "technique_store")

/**
 * The classic, most popular reality check methods. Seeded once on first run
 * so the banner has a full library of ideas out of the box.
 */
val SEEDED_TECHNIQUES = listOf(
    "Count your fingers — in dreams you may find extra, missing, or distorted ones",
    "Pinch your nose shut and try to breathe — in a dream, you'll still be able to breathe",
    "Read text, look away, then read it again — dream text often changes",
    "Glance at a clock or your phone screen twice — dream time and digits shift",
    "Flick a light switch — dream lights often refuse to change",
    "Study your hands closely — they often look warped or unfamiliar in dreams",
    "Push a finger firmly into your palm — in a dream it may pass right through",
    "Look into a mirror — dream reflections are often blurred, wrong, or absent",
    "Ask yourself: how did I get here? — dreams rarely have a coherent path",
    "Jump — in dreams you may float, hover, or drift down in slow motion",
    "Try running — dream running often feels like wading through water",
    "Do simple math in your head — dream arithmetic tends to fail",
    "Try reading small or fine print — dream text blurs when you focus",
    "Pinch or bite your arm — dream sensations feel distant or delayed",
    "Try singing or humming aloud — dream voices can be slurred or silent",
    "Examine a detailed texture (leaves, brick, fabric) — dream detail wobbles",
    "Look at your feet and shoes — they're often oddly shaped or mismatched",
    "Recall a specific recent memory in detail — dream memories dissolve",
    "Try to push through a closed door — dream physics may let you",
    "Look up at the sky twice — dream skies and stars rearrange themselves",
)

/**
 * Persists reality check techniques in DataStore as JSON.
 * Same pattern as [AdviceRepository]; additionally seeds the classic
 * methods once on first run.
 */
class TechniqueRepository(private val context: Context) {

    private val keyTechniques = stringPreferencesKey("techniques_json")
    private val keySeeded = booleanPreferencesKey("has_seeded")

    /** Observe all techniques (reactive). Deduplicates IDs on read. */
    val allTechniquesFlow: Flow<List<TechniqueItem>> = context.techniqueDataStore.data.map { prefs ->
        loadAll(prefs)
    }

    /**
     * Inserts [SEEDED_TECHNIQUES] once, on first run only. A separate
     * `has_seeded` flag (not list emptiness) guards this, so deleting all
     * the classics later does not resurrect them.
     */
    suspend fun seedIfEmpty() {
        context.techniqueDataStore.edit { prefs ->
            if (prefs[keySeeded] != true) {
                val current = loadAll(prefs)
                val seeds = SEEDED_TECHNIQUES.map { TechniqueItem(text = it, isSeeded = true) }
                prefs[keyTechniques] = Json.encodeToString(current + seeds)
                prefs[keySeeded] = true
            }
        }
    }

    /** Add a new user technique. */
    suspend fun add(text: String) {
        context.techniqueDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val item = TechniqueItem(text = text.trim(), isSeeded = false)
            prefs[keyTechniques] = Json.encodeToString(current + item)
        }
    }

    /** Update an existing technique's text. */
    suspend fun update(item: TechniqueItem, newText: String) {
        context.techniqueDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val updated = current.map { if (it.id == item.id) it.copy(text = newText.trim()) else it }
            prefs[keyTechniques] = Json.encodeToString(updated)
        }
    }

    /** Delete a technique by id. */
    suspend fun delete(id: Long) {
        context.techniqueDataStore.edit { prefs ->
            val current = loadAll(prefs)
            prefs[keyTechniques] = Json.encodeToString(current.filter { it.id != id })
        }
    }

    /**
     * Loads all techniques and fixes any duplicate IDs that may exist from
     * earlier versions where `System.currentTimeMillis()` could collide.
     */
    private fun loadAll(prefs: Preferences): List<TechniqueItem> {
        val json = prefs[keyTechniques] ?: return emptyList()
        val raw = runCatching { Json.decodeFromString<List<TechniqueItem>>(json) }.getOrDefault(emptyList())
        // Deduplicate IDs — reassign new unique IDs to any collisions
        val seen = mutableSetOf<Long>()
        return raw.map { item ->
            if (seen.add(item.id)) item
            else item.copy(id = techniqueNextId.getAndIncrement())
        }
    }
}
