package com.example.vild.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * One day's reality check record.
 *
 * Created each morning (by the 8 AM alarm, or on app open if none exists yet)
 * with a randomly chosen trigger. The app then *insists* — via [readAt] and
 * [doneAt] tracking and a nagging notification — until the user confirms they
 * have both READ and DONE the check.
 */
@Serializable
data class RealityCheckDayLog(
    /** Local-zone day this log belongs to (LocalDate.toEpochDay()). */
    val epochDay: Long,
    /** The chosen trigger's id (0 when the fallback prompt was used). */
    val triggerId: Long,
    /** Snapshot of the trigger text — survives trigger edits/deletes. */
    val triggerText: String,
    /** When this trigger was chosen for the day. */
    val chosenAt: Long = System.currentTimeMillis(),
    /** When the user confirmed they read it; null until then. */
    val readAt: Long? = null,
    /** When the user confirmed they actually did the check; null until then. */
    val doneAt: Long? = null,
    /**
     * How many times the user tapped "I read it" — the button is deliberately
     * repeatable so extra rounds can be logged in the Tail app; the first
     * tap is what sets [readAt].
     */
    val readCount: Int = 0,
    /**
     * How many times the user tapped "I did it" — the button is deliberately
     * repeatable so extra rounds can be logged in the Tail app; the first
     * tap is what sets [doneAt] and completes the day.
     */
    val doneCount: Int = 0,
) {
    /** True once the user has confirmed both reading and doing the check. */
    val isComplete: Boolean get() = readAt != null && doneAt != null

    /**
     * How many practice rounds are still needed today to hit the daily goals
     * ([DAILY_READ_GOAL] reads + [DAILY_DONE_GOAL] dones). Drives the adaptive
     * nag frequency: more remaining rounds → more frequent notifications.
     */
    val unitsRemaining: Int
        get() = (DAILY_READ_GOAL - readCount).coerceAtLeast(0) +
            (DAILY_DONE_GOAL - doneCount).coerceAtLeast(0)

    /** True once both daily goals are hit — no more nagging needed today. */
    val goalsMet: Boolean get() = readCount >= DAILY_READ_GOAL && doneCount >= DAILY_DONE_GOAL
}

/** Daily goal: how many times the user wants to READ the trigger. */
const val DAILY_READ_GOAL = 5

/** Daily goal: how many times the user wants to DO the trigger. */
const val DAILY_DONE_GOAL = 2

/** Today's epoch day in the local time zone. */
fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** Fallback prompt used when the user has no triggers configured yet. */
const val FALLBACK_TRIGGER_TEXT = "Look at your hands. Count your fingers. Are you dreaming?"
