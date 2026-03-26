package com.sleekydz86.idolglow.global.aop.concurrency.distribution

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val prefix: String = "lock",
    val waitTimeMillis: Long = 1_000,
    val leaseTimeMillis: Long = 10_000
)
