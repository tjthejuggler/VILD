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
) {
    /** True once the user has confirmed both reading and doing the check. */
    val isComplete: Boolean get() = readAt != null && doneAt != null
}

/** Today's epoch day in the local time zone. */
fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** Fallback prompt used when the user has no triggers configured yet. */
const val FALLBACK_TRIGGER_TEXT = "Look at your hands. Count your fingers. Are you dreaming?"
