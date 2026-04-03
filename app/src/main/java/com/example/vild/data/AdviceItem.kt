package com.example.vild.data

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/** Monotonic counter seeded from the current time to guarantee unique IDs. */
internal val nextId = AtomicLong(System.currentTimeMillis())

/**
 * A user-entered piece of advice associated with a section ("day" or "night").
 *
 * Stored as JSON in DataStore (no Room dependency needed).
 */
@Serializable
data class AdviceItem(
    val id: Long = nextId.getAndIncrement(),
    val section: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
)
