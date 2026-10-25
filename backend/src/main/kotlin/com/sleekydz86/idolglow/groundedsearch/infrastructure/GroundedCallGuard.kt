package com.sleekydz86.idolglow.groundedsearch.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

@Component
class GroundedCallGuard {
    private val failures = AtomicInteger(0)
    private val openedAt = AtomicLong(0)
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> run(name: String, block: () -> T): T {
        if (open()) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 외부 호출이 잠시 막혀 있습니다.")
        }
        return invoke(name, System.nanoTime(), block)
    }

    private fun <T> invoke(name: String, started: Long, block: () -> T): T {
        val result = runCatching { block() }
        val error = result.exceptionOrNull()
        if (error != null) {
            fail(name, error)
        }
        failures.set(0)
        log.info("근거 검색 {} 지연 ms={}", name, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
        return result.getOrThrow()
    }

    private fun fail(name: String, error: Throwable): Nothing {
        if (failures.incrementAndGet() >= 2) {
            openedAt.set(System.nanoTime())
        }
        log.warn("근거 검색 {} 호출 실패 type={}", name, error.javaClass.simpleName, error)
        if (error is ResponseStatusException) {
            throw error
        }
        throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 외부 호출에 실패했습니다.", error)
    }

    private fun open(): Boolean {
        val opened = openedAt.get()
        val cooling = opened != 0L && System.nanoTime() - opened < cooldownNanos()
        if (!cooling && opened != 0L) {
            openedAt.set(0)
            failures.set(1)
        }
        return cooling
    }

    private fun cooldownNanos(): Long = Duration.ofSeconds(((2 shl 2) + 2).toLong()).toNanos()
}
