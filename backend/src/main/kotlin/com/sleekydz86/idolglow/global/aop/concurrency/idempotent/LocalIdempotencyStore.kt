package com.sleekydz86.idolglow.global.aop.concurrency.idempotent

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class LocalIdempotencyStore : IdempotencyStore {
    private data class LocalEntry(
        val future: CompletableFuture<Any?>,
        val expiresAt: Long
    )

    private val store = ConcurrentHashMap<String, LocalEntry>()

    override fun getOrCreate(key: String, ttlMillis: Long): IdempotencyEntry {
        val now = System.currentTimeMillis()
        var isOwner = false
        val entry = store.compute(key) { _, existing ->
            if (existing == null || existing.expiresAt <= now) {
                isOwner = true
                LocalEntry(CompletableFuture(), now + ttlMillis)
            } else {
                existing
            }
        } ?: LocalEntry(CompletableFuture(), now + ttlMillis)

        return IdempotencyEntry(entry.future, isOwner)
    }

    override fun remove(key: String) {
        store.remove(key)
    }
}
