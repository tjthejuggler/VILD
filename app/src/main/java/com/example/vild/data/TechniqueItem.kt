package com.example.vild.data

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/** Monotonic counter seeded from the current time to guarantee unique IDs. */
internal val techniqueNextId = AtomicLong(System.currentTimeMillis() + 20_000)

/**
 * A reality check technique — an idea for *how* to test whether you are
 * dreaming (count fingers, pinch your nose, re-read text, …).
 *
 * The store ships pre-seeded with the classic methods ([isSeeded] = true);
 * the user can add, edit and delete their own.
 */
@Serializable
data class TechniqueItem(
    val id: Long = techniqueNextId.getAndIncrement(),
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** True for the built-in classic methods; false for user-added ones. */
    val isSeeded: Boolean = false,
)
