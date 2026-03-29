package com.sleekydz86.idolglow.global.aop.concurrency.idempotent

import com.sleekydz86.idolglow.global.aop.concurrency.KeyExpressionResolver
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Aspect
class IdempotencyAspect(
    private val idempotencyStore: IdempotencyStore,
    private val keyResolver: KeyExpressionResolver
) {

    @Around("@annotation(idempotent)")
    fun around(joinPoint: ProceedingJoinPoint, idempotent: Idempotent): Any? {
        val resolvedKey = keyResolver.resolve(idempotent.key, joinPoint)
        val storeKey = "${idempotent.prefix}:$resolvedKey"
        val entry = idempotencyStore.getOrCreate(storeKey, idempotent.ttlMillis)

        if (entry.isOwner) {
            return try {
                val result = joinPoint.proceed()
                entry.future.complete(result)
                result
            } catch (ex: Exception) {
                entry.future.completeExceptionally(ex)
                idempotencyStore.remove(storeKey)
                throw ex
            }
        }

        return try {
            entry.future.get(idempotent.waitTimeoutMillis, TimeUnit.MILLISECONDS)
        } catch (ex: TimeoutException) {
            throw IllegalStateException("멱등성 대기 시간이 초과되었습니다. 식별키=$storeKey", ex)
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("멱등성 대기가 중단되었습니다. 식별키=$storeKey", ex)
        } catch (ex: ExecutionException) {
            val cause = ex.cause
            if (cause is RuntimeException) {
                throw cause
            }
            throw IllegalStateException("멱등성 처리 중 실행에 실패했습니다. 식별키=$storeKey", cause)
        }
    }
}
