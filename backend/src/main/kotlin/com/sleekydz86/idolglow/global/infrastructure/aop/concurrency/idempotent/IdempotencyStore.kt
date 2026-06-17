package com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.idempotent

interface IdempotencyStore {
    fun getOrCreate(key: String, ttlMillis: Long): IdempotencyEntry
    fun remove(key: String)
}
