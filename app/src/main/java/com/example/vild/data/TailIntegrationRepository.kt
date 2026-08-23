package com.example.vild.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A habit exposed by the Tail app's Content Provider. */
data class TailHabit(
    val habitId: String,
    val habitName: String,
)

/**
 * Handles all IPC communication with the Tail habit-tracking app — the same
 * protocol the WAGS app uses:
 *
 *  • Habit list is read from Tail's Content Provider
 *    (`content://com.example.tail.provider/habits`).
 *  • Increments are explicit, permission-guarded broadcasts
 *    (`com.example.tail.ACTION_INCREMENT_HABIT`) carrying the habit *name*.
 *  • Retroactive backfill uses `com.example.tail.ACTION_SET_HABIT_VALUES`
 *    with a `{"yyyy-MM-dd": <count>}` JSON map (SET semantics, idempotent).
 *
 * VILD has two slots:
 *  • [Slot.READ] — fired every single time the user taps "I read it" (the
 *    button is intentionally repeatable so extra rounds land in Tail).
 *  • [Slot.DONE] — fired every single time the user taps "I did it" (the
 *    button is intentionally repeatable so extra rounds land in Tail).
 */
class TailIntegrationRepository(private val context: Context) {

    enum class Slot(
        val idKey: String,
        val nameKey: String,
        val label: String,
    ) {
        READ(
            idKey = "tail_habit_id_read",
            nameKey = "tail_habit_name_read",
            label = "Reality Check Read",
        ),
        DONE(
            idKey = "tail_habit_id_done",
            nameKey = "tail_habit_name_done",
            label = "Reality Check Done",
        ),
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vild_tail_prefs", Context.MODE_PRIVATE)

    // ── Habit discovery ────────────────────────────────────────────────────────

    /**
     * Queries Tail's Content Provider for all habits. Returns an empty list
     * (rather than throwing) when Tail is not installed or refuses the query.
     */
    suspend fun fetchHabits(): List<TailHabit> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TailHabit>()
        try {
            context.contentResolver.query(
                HABITS_CONTENT_URI,
                arrayOf(COL_HABIT_ID, COL_HABIT_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(COL_HABIT_NAME)
                if (nameIdx >= 0) {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        if (name.isBlank()) continue
                        // Use the habit name as the ID — the Tail receiver accepts a
                        // name string for EXTRA_HABIT_ID and it survives reordering.
                        results += TailHabit(habitId = name, habitName = name)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchHabits: could not query Tail app — ${e.message}")
        }
        results
    }

    // ── Per-slot persistence ───────────────────────────────────────────────────

    /** Returns the persisted habit name for [slot], or "" if none selected. */
    fun getHabitName(slot: Slot): String =
        prefs.getString(slot.nameKey, "") ?: ""

    /** Persists the selected habit name for [slot]. */
    fun setHabit(slot: Slot, habitName: String) {
        prefs.edit()
            .putString(slot.idKey, habitName)
            .putString(slot.nameKey, habitName)
            .apply()
    }

    /** Clears the selection for [slot]. */
    fun clearHabit(slot: Slot) {
        prefs.edit()
            .putString(slot.idKey, "")
            .putString(slot.nameKey, "")
            .apply()
    }

    // ── Live increments ────────────────────────────────────────────────────────

    /**
     * Sends an explicit, permission-guarded broadcast asking Tail to increment
     * the habit mapped to [slot] by one. Does nothing if no habit is selected.
     * Never crashes the host app if Tail is missing.
     */
    fun sendHabitIncrement(slot: Slot) {
        val habitName = getHabitName(slot)
        if (habitName.isBlank()) {
            Log.d(TAG, "sendHabitIncrement(${slot.name}): no habit selected, skipping")
            return
        }
        try {
            val intent = Intent(ACTION_INCREMENT).apply {
                // Explicit broadcast — required for reliable delivery on API 26+.
                `package` = HABIT_APP_PACKAGE
                putExtra(EXTRA_HABIT_ID, habitName)
                putExtra(EXTRA_SLOT, slot.name)
                // Tag the originator so Tail's ACTION_HABIT_INCREMENTED
                // announcement of this same increment can be recognised as an
                // echo by TailHabitSyncReceiver (bidirectional loop safety).
                putExtra(EXTRA_SOURCE, context.packageName)
            }
            // receiverPermission ensures only the Tail app (which declared the
            // signature permission) can receive this broadcast.
            context.sendBroadcast(intent, PERMISSION_TAIL)
            Log.d(TAG, "sendHabitIncrement(${slot.name}): fired for habitId=$habitName")
        } catch (e: SecurityException) {
            // Android 14+ throws when the receiver permission is defined by no
            // installed app — i.e. Tail is not installed.
            Log.w(TAG, "sendHabitIncrement(${slot.name}): SecurityException — " +
                "Tail app likely not installed. ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "sendHabitIncrement(${slot.name}): unexpected error — ${e.message}")
        }
    }

    // ── Retroactive backfill ───────────────────────────────────────────────────

    /**
     * Sends a SET broadcast asking Tail to **replace** the stored value for
     * each date in [dateValues] for the habit mapped to [slot].
     *
     * Keys are ISO-8601 dates (`yyyy-MM-dd`); values are the day's total
     * (1 for a read, the done-round count for a done). Idempotent — running
     * the backfill twice produces the same result.
     */
    fun sendHabitValuesForDates(slot: Slot, dateValues: Map<String, Int>) {
        val habitName = getHabitName(slot)
        if (habitName.isBlank()) {
            Log.d(TAG, "sendHabitValuesForDates(${slot.name}): no habit selected, skipping")
            return
        }
        if (dateValues.isEmpty()) {
            Log.d(TAG, "sendHabitValuesForDates(${slot.name}): empty map, skipping")
            return
        }

        val json = buildString {
            append("{")
            dateValues.entries.forEachIndexed { i, (date, value) ->
                if (i > 0) append(",")
                append("\"").append(date).append("\":").append(value)
            }
            append("}")
        }

        try {
            val intent = Intent(ACTION_SET_HABIT_VALUES).apply {
                `package` = HABIT_APP_PACKAGE
                putExtra(EXTRA_HABIT_ID, habitName)
                putExtra(EXTRA_SLOT, slot.name)
                putExtra(EXTRA_VALUES_JSON, json)
            }
            context.sendBroadcast(intent, PERMISSION_TAIL)
            Log.d(TAG, "sendHabitValuesForDates(${slot.name}): fired for habitId=$habitName, " +
                "${dateValues.size} dates")
        } catch (e: SecurityException) {
            Log.w(TAG, "sendHabitValuesForDates(${slot.name}): SecurityException — " +
                "Tail app likely not installed. ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "sendHabitValuesForDates(${slot.name}): unexpected error — ${e.message}")
        }
    }

    companion object {
        private const val TAG = "TailIntegrationRepo"

        /** Package name of the Tail habit-tracking app. */
        const val HABIT_APP_PACKAGE = "com.example.tail"

        /** Content Provider URI exposed by the Tail app. */
        val HABITS_CONTENT_URI: Uri =
            Uri.parse("content://com.example.tail.provider/habits")

        /** Column names returned by the Tail app's Content Provider. */
        const val COL_HABIT_ID = "habit_id"
        const val COL_HABIT_NAME = "habit_name"

        /** Broadcast action Tail's HabitIncrementReceiver listens for. */
        const val ACTION_INCREMENT = "com.example.tail.ACTION_INCREMENT_HABIT"

        /** Extra key: the habit name (String). */
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"

        /** Extra key carrying the originating VILD slot name (informational). */
        const val EXTRA_SLOT = "vild_slot"

        /** Broadcast action Tail fires after EVERY successful habit increment. */
        const val ACTION_HABIT_INCREMENTED = "com.example.tail.ACTION_HABIT_INCREMENTED"

        /** Extra on ACTION_HABIT_INCREMENTED: the incremented habit's name. */
        const val EXTRA_HABIT_NAME = "EXTRA_HABIT_NAME"

        /** Extra on ACTION_HABIT_INCREMENTED: the applied count delta (0 = no-op). */
        const val EXTRA_AMOUNT = "EXTRA_AMOUNT"

        /** Extra identifying the originating app; VILD tags its own increments. */
        const val EXTRA_SOURCE = "EXTRA_SOURCE"

        /** Broadcast action for the idempotent retroactive backfill. */
        const val ACTION_SET_HABIT_VALUES = "com.example.tail.ACTION_SET_HABIT_VALUES"

        /** Extra key: JSON object `{"yyyy-MM-dd": <count:Int>, ...}`. */
        const val EXTRA_VALUES_JSON = "EXTRA_VALUES_JSON"

        /** Signature-level permission declared by the Tail app. */
        const val PERMISSION_TAIL = "com.example.tail.permission.TAIL_INTEGRATION"
    }
}
