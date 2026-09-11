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

private val Context.triggerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reality_check_store")

/**
 * Starter library of daily reality check prompts, seeded once on first run.
 * Draws on LaBerge's prospective-memory "state tests" (checking at meaningful
 * moments), community dream-sign practice (r/LucidDreaming, Dream Views) and
 * the false-awakening protocol (check every time you wake up).
 */
val SEEDED_TRIGGERS = listOf(
    // Core questions
    "Am I dreaming right now? Prove it — run two checks.",
    "Stop. Really look around. Could this be a dream?",
    "How exactly did I get here? Trace your last hour.",
    "What was I doing ten minutes ago? Remember it clearly.",
    "Assume you're dreaming until reality proves otherwise. Check now.",
    "What would be impossible right now? Test whether it happens.",
    "If this were a dream, what would you do next? First — check.",
    "Question one solid thing around you. Does it survive scrutiny?",
    // Moment-anchored (prospective memory targets)
    "Every doorway today is a reminder: pass through and check reality.",
    "Next time you unlock your phone, count your fingers first.",
    "When you next see a clock, look at it twice — did the time behave?",
    "Next mirror you pass: is your reflection really you?",
    "Next time you drink or pour something, question the whole scene.",
    "Next time you switch a light on or off, watch what actually happens.",
    "The next message you send: re-read it after. Did the words hold still?",
    "When you next sit down, ask how long you've been here — and how you arrived.",
    "Next time you hear music or speech, listen closely — is it coherent?",
    "Next time you step outside, verify the sky, the light, the weather.",
    "Next time you wash your hands, push a finger into your palm too.",
    "When you next touch a doorknob or railing, is the texture exactly right?",
    // Waking protocol (false-awakening defense)
    "Just woke up — even at 3 a.m.? Do a nose pinch before trusting it.",
    "Felt sure you woke up? False awakenings feel certain. Verify twice.",
    "Morning protocol: before getting up, run two full reality checks.",
    // Dream-sign prompts
    "Anyone here who shouldn't exist? People from the past are a dream sign.",
    "Does this place make sense? Childhood homes are classic dream sets.",
    "Are the people around you acting strangely? Notice, then check.",
    "Anything odd happening that you're ignoring? Dreams count on that.",
    "Does the layout of this place match reality? Walk it back in your mind.",
    "Déjà vu right now? That's a reason to check, not a parlor trick.",
    "Look at your hands — dream hands are almost never quite right.",
    "Check your shoes and clothes — dreams get them subtly wrong.",
    // Perception & physics probes
    "Read a sentence, look away, read it again. Did the words survive?",
    "Try simple arithmetic in your head. Dream math quietly fails.",
    "Jump. Did gravity behave exactly as it should?",
    "Flick the nearest switch. Did the light actually obey?",
    "Pick an object, look away, look back. Is it still the same object?",
    "Push your finger into your palm and expect it to sink in. Really expect it.",
    "Study any fine detail nearby — dream detail wobbles under attention.",
    "Try to hum or whistle a tune — dream sound drifts off-key or cuts out.",
    // Metacognition
    "When did you last feel fully present? Awareness now prevents autopilot.",
    "Are you thinking clearly, or does everything feel foggy and acceptable?",
    "What are you expecting to happen next? Dreams run on expectation.",
    "Notice one thing you'd normally ignore. Question its existence.",
    "If a character in your dream lived this exact moment, would they know?",
    "Your dreams borrow today's moments. Could this one be borrowed?",
    "What's the strangest thing in this scene? Start your check there.",
    "Reality is consistent. Find the inconsistency — if there is one.",
    "Slow down for ten seconds. Dreams rush you past the exits.",
    "Read this twice, carefully. Text is fragile in dreams.",
    "Do one check now, and one more in an hour. Promise yourself.",
)

/**
 * Persists reality check triggers in DataStore as JSON.
 * Same pattern as [AdviceRepository].
 */
class RealityCheckRepository(private val context: Context) {

    private val keyTriggers = stringPreferencesKey("triggers_json")
    private val keySeeded = booleanPreferencesKey("triggers_seeded")

    /** Observe all triggers (reactive). Deduplicates IDs on read. */
    val allTriggersFlow: Flow<List<RealityCheckTrigger>> = context.triggerDataStore.data.map { prefs ->
        loadAll(prefs)
    }

    /**
     * Seeds [SEEDED_TRIGGERS] once, on first run only. A separate flag guards
     * this, so deleting the starter prompts later does not resurrect them.
     * Exact-text dedup keeps any user-entered duplicates from doubling up.
     */
    suspend fun seedIfEmpty() {
        context.triggerDataStore.edit { prefs ->
            if (prefs[keySeeded] != true) {
                val current = loadAll(prefs)
                val known = current.mapTo(mutableSetOf()) { it.text }
                val missing = SEEDED_TRIGGERS
                    .filterNot { it in known }
                    .map { RealityCheckTrigger(text = it) }
                if (missing.isNotEmpty()) {
                    prefs[keyTriggers] = Json.encodeToString(current + missing)
                }
                prefs[keySeeded] = true
            }
        }
    }

    /** Add a new trigger, ignoring exact duplicates. */
    suspend fun add(text: String) {
        val trimmed = text.trim()
        context.triggerDataStore.edit { prefs ->
            val current = loadAll(prefs)
            if (current.none { it.text == trimmed }) {
                prefs[keyTriggers] = Json.encodeToString(current + RealityCheckTrigger(text = trimmed))
            }
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
