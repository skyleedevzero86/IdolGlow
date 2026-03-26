package com.sleekydz86.idolglow.global.aop.concurrency.idempotent

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent(
    val key: String,
    val prefix: String = "idempotency",
    val ttlMillis: Long = 60_000,
    val waitTimeoutMillis: Long = 5_000
)
