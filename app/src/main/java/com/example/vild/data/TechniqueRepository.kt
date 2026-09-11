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
 * The expanded library, seeded in a second one-time pass so existing installs
 * receive it too. Sourced from r/LucidDreaming community lists, LucidWiki,
 * World of Lucid Dreaming, oneironauts.io and The Lucid Guide (LaBerge's
 * "critical state testing"). No duplicates of [SEEDED_TECHNIQUES].
 */
val EXPANDED_TECHNIQUES = listOf(
    "Pull gently on one finger — dream fingers may stretch or even come off",
    "Check your pulse at your neck or wrist — dreams rarely bother to simulate one",
    "Flip a coin and try to stop it mid-air with your mind — dream physics allows it",
    "Link two fingers into hoops and pull softly — dream touch often goes completely numb",
    "Draw a mark or letter on your hand — in a dream it may change or vanish when you check",
    "Play a song or an instrument — dream music skips, loops, or adds sounds from nowhere",
    "Dunk your face in water and inhale — dream water breathes like air (verify you're awake first!)",
    "Pull the skin on your arm — dream skin stretches unrealistically far",
    "Pinch your nose and try to blow out, like popping your ears — dream air flows right through",
    "Push your hand through a wall or desk — genuinely expect it to sink in",
    "Try to lift a small object with your mind — dream telekinesis often just works",
    "Ask another person: am I dreaming? — dream characters often say yes, or hesitate",
    "Open your phone's home screen, look away, look back — dream apps rearrange themselves",
    "Try to hover or lift off the ground slightly — willing flight is a classic expectation test",
    "Stare into a mirror in dim light and expect your face to change — in dreams it will",
    "Ask: is my body normal? Do I even have one? — dream bodies are approximate",
    "Pick an object, look away, look back — dream objects move, change, or disappear",
    "Try to blur or sharpen your vision at will — dream vision obeys expectation",
    "Look at your phone wallpaper — the dream version is almost never yours",
    "Listen closely to the background sound — in dreams it crackles, loops, or distorts",
    "Dial a number or type a message — dream devices garble your input",
    "Check your clothing twice — dream colors, patterns, and layers refuse to stay consistent",
    "Study a bookshelf, painting, or poster twice — dream artwork rearranges under attention",
    "Press on the nearest solid object — dream surfaces may yield or let fingers sink in",
    "Leave a room and immediately re-enter it — dream layouts rarely survive the trip",
    "Turn on a faucet — dream water pressure, temperature, and flow misbehave",
    "Give every check ten genuine seconds — assume you're dreaming until proven awake",
    "Always pair two checks back-to-back (nose pinch + text) — any single check can fail",
    "Scan the scene for one impossible thing — dreams always hide at least one",
    "Ask: does this place make sense? — childhood homes and schools are classic dream sets",
    "Look for people who shouldn't be there — the departed, celebrities, old classmates",
    "Watch the people around you — dream characters repeat phrases and act strangely",
    "Check your shadow — dream shadows detach, lag, or point the wrong way",
    "Turn a volume dial or thermostat — dream controls barely respond",
    "Count a handful of coins or bills twice — dream money never adds up the same",
    "Glance at a calendar or your phone's date twice — dream dates are impossible or shift",
    "Question the weather — snow in summer or sun at midnight means you're dreaming",
    "Run a hand over your hair and face — dream length and texture are often wrong",
    "Try to speak a language you barely know — dream fluency is a giveaway",
    "Notice your emotions — sudden euphoria or dread with no cause is a dream sign",
    "Glance at an analog watch — dream hands spin wildly or stand perfectly still",
    "Toggle your phone's flashlight — the dream beam is weak, absent, or points nowhere",
)

/**
 * Persists reality check techniques in DataStore as JSON.
 * Same pattern as [AdviceRepository]; additionally seeds the classic
 * methods once on first run.
 */
class TechniqueRepository(private val context: Context) {

    private val keyTechniques = stringPreferencesKey("techniques_json")
    private val keySeeded = booleanPreferencesKey("has_seeded")
    private val keySeededExpanded = booleanPreferencesKey("has_seeded_expanded_v2")

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

    /**
     * Seeds [EXPANDED_TECHNIQUES] in a separate one-time pass (own flag), so
     * devices that already received the original 20 classics still pick up
     * the expanded library. Skips entries the user already has (by exact
     * text) so user-edited or deleted classics are never resurrected.
     */
    suspend fun seedExpandedIfAbsent() {
        context.techniqueDataStore.edit { prefs ->
            if (prefs[keySeededExpanded] != true) {
                val current = loadAll(prefs)
                val known = current.mapTo(mutableSetOf()) { it.text }
                val missing = EXPANDED_TECHNIQUES
                    .filterNot { it in known }
                    .map { TechniqueItem(text = it, isSeeded = true) }
                if (missing.isNotEmpty()) {
                    prefs[keyTechniques] = Json.encodeToString(current + missing)
                }
                prefs[keySeededExpanded] = true
            }
        }
    }

    /** Add a new user technique, ignoring exact duplicates. */
    suspend fun add(text: String) {
        val trimmed = text.trim()
        context.techniqueDataStore.edit { prefs ->
            val current = loadAll(prefs)
            if (current.none { it.text == trimmed }) {
                prefs[keyTechniques] = Json.encodeToString(current + TechniqueItem(text = trimmed, isSeeded = false))
            }
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
