package io.github.dharma_labs.openeye.domain

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory, per-surface allowance created when the user explicitly chooses
 * "Open anyway" on the intentional block screen. Cleared whenever strict
 * enforcement (focus or schedule) takes over.
 */
object BypassStore {
    private val allowances = ConcurrentHashMap<String, Long>()

    fun key(platformId: String, surfaceId: String) = "$platformId/$surfaceId"

    fun allow(platformId: String, surfaceId: String, minutes: Int, now: Long = System.currentTimeMillis()) {
        allowances[key(platformId, surfaceId)] = now + minutes * 60_000L
    }

    fun until(platformId: String, surfaceId: String): Long? {
        val key = key(platformId, surfaceId)
        val until = allowances[key] ?: return null
        if (until <= System.currentTimeMillis()) {
            allowances.remove(key)
            return null
        }
        return until
    }

    fun clear() = allowances.clear()
}