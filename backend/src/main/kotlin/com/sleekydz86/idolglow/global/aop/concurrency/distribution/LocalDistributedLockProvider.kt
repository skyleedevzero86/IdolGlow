package com.sleekydz86.idolglow.global.aop.concurrency.distribution

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class LocalDistributedLockProvider : DistributedLockProvider {

    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    override fun tryLock(
        key: String,
        waitTimeMillis: Long,
        leaseTimeMillis: Long
    ): LockHandle? {
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        return try {
            val acquired = if (waitTimeMillis <= 0) {
                lock.tryLock()
            } else {
                lock.tryLock(waitTimeMillis, TimeUnit.MILLISECONDS)
            }

            if (acquired) {
                LockHandle(key, UUID.randomUUID().toString())
            } else {
                null
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
    }

    override fun release(handle: LockHandle) {
        val lock = locks[handle.key] ?: return
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
            if (!lock.isLocked && !lock.hasQueuedThreads()) {
                locks.remove(handle.key, lock)
            }
        }
    }
}
