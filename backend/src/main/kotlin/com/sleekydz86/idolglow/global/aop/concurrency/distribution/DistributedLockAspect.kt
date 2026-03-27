package com.sleekydz86.idolglow.global.aop.concurrency.distribution

import com.sleekydz86.idolglow.global.aop.concurrency.KeyExpressionResolver
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

@Aspect
class DistributedLockAspect(
    private val lockProvider: DistributedLockProvider,
    private val keyResolver: KeyExpressionResolver
) {

    @Around("@annotation(distributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
        val resolvedKey = keyResolver.resolve(distributedLock.key, joinPoint)
        val lockKey = "${distributedLock.prefix}:$resolvedKey"
        val handle = lockProvider.tryLock(
            lockKey,
            distributedLock.waitTimeMillis,
            distributedLock.leaseTimeMillis
        ) ?: throw IllegalStateException("락 획득에 실패했습니다. 식별키=$lockKey")

        return try {
            joinPoint.proceed()
        } finally {
            lockProvider.release(handle)
        }
    }
}
