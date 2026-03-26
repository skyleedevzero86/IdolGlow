package com.sleekydz86.idolglow.global.aop.concurrency.idempotent

import java.util.concurrent.CompletableFuture

data class IdempotencyEntry(
    val future: CompletableFuture<Any?>,
    val isOwner: Boolean
)

interface IdempotencyStore {
    fun getOrCreate(key: String, ttlMillis: Long): IdempotencyEntry
    fun remove(key: String)
}
