package com.sleekydz86.idolglow.global.infrastructure.aop.concurrency

import com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.distribution.DistributedLockAspect
import com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.distribution.DistributedLockProvider
import com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.distribution.LocalDistributedLockProvider
import com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.idempotent.IdempotencyAspect
import com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.idempotent.IdempotencyStore
import com.sleekydz86.idolglow.global.infrastructure.aop.concurrency.idempotent.LocalIdempotencyStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ConcurrencyAopConfig {

    @Bean
    @ConditionalOnMissingBean
    fun distributedLockProvider(): DistributedLockProvider = LocalDistributedLockProvider()

    @Bean
    @ConditionalOnMissingBean
    fun idempotencyStore(): IdempotencyStore = LocalIdempotencyStore()

    @Bean
    @ConditionalOnMissingBean
    fun keyExpressionResolver(): KeyExpressionResolver = KeyExpressionResolver()

    @Bean
    @ConditionalOnMissingBean
    fun distributedLockAspect(
        lockProvider: DistributedLockProvider,
        keyExpressionResolver: KeyExpressionResolver
    ): DistributedLockAspect = DistributedLockAspect(lockProvider, keyExpressionResolver)

    @Bean
    @ConditionalOnMissingBean
    fun idempotencyAspect(
        idempotencyStore: IdempotencyStore,
        keyExpressionResolver: KeyExpressionResolver
    ): IdempotencyAspect = IdempotencyAspect(idempotencyStore, keyExpressionResolver)
}
