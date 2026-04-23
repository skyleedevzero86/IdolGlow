package com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.distribution

data class LockHandle(
    val key: String,
    val lockId: String
)

interface DistributedLockProvider {
    fun tryLock(key: String, waitTimeMillis: Long, leaseTimeMillis: Long): LockHandle?
    fun release(handle: LockHandle)
}
