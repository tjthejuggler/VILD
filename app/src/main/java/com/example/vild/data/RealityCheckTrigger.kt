package com.example.vild.data

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/** Monotonic counter seeded from the current time to guarantee unique IDs. */
internal val triggerNextId = AtomicLong(System.currentTimeMillis() + 10_000)

/**
 * A user-entered reality check trigger.
 *
 * Stored as JSON in DataStore (no Room dependency needed).
 * One is randomly chosen each morning and shown as a notification.
 */
@Serializable
data class RealityCheckTrigger(
    val id: Long = triggerNextId.getAndIncrement(),
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)
