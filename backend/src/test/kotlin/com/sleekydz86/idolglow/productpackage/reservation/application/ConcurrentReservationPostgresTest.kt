package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("동시 예약 PostgreSQL")
class ConcurrentReservationPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("같은 슬롯에 10명이 동시에 예약하면 1명만 성공한다")
    @Test
    fun onlyOneOfTenConcurrentReservationsSucceeds() {
        // given
        val slot = fixture.persistOpenSlot()
        val users = List(10) { fixture.persistUser() }
        val start = CountDownLatch(1)
        val done = CountDownLatch(users.size)
        val success = AtomicInteger()
        val pool = Executors.newFixedThreadPool(users.size)
        users.forEach { user ->
            pool.submit {
                start.await()
                try {
                    fixture.createReservation(user.id, slot)
                    success.incrementAndGet()
                } catch (_: Exception) {
                } finally {
                    done.countDown()
                }
            }
        }

        // when
        start.countDown()
        done.await(30, TimeUnit.SECONDS)
        pool.shutdownNow()

        // then
        val reserved =
            jdbcTemplate.queryForObject(
                "select count(*) from reservations where reservation_slot_id = ?",
                Long::class.java,
                slot.slotId,
            )
        assertEquals(1, success.get())
        assertEquals(1L, reserved)
    }

    @DisplayName("T-6 같은 슬롯에 100명이 동시에 예약해도 1명만 성공하고, 나머지 99명은 모두 '이미 선점' 거절을 받는다")
    @Test
    fun hundredConcurrentReservationsAllowOnlyOne() {
        // given
        val slot = fixture.persistOpenSlot()
        val users = List(HUNDRED) { fixture.persistUser() }
        val start = CountDownLatch(1)
        val done = CountDownLatch(users.size)
        val success = AtomicInteger()
        val failures = ConcurrentLinkedQueue<String>()
        val pool = Executors.newFixedThreadPool(users.size)
        users.forEach { user ->
            pool.submit {
                start.await()
                runCatching { fixture.createReservation(user.id, slot) }
                    .onSuccess { success.incrementAndGet() }
                    .onFailure { failures += "${it.javaClass.simpleName}: ${it.message}" }
                done.countDown()
            }
        }

        // when
        val startedAt = System.nanoTime()
        start.countDown()
        val finished = done.await(60, TimeUnit.SECONDS)
        val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI
        pool.shutdownNow()

        // then
        val reserved =
            jdbcTemplate.queryForObject(
                "select count(*) from reservations where reservation_slot_id = ?",
                Long::class.java,
                slot.slotId,
            )
        assertTrue(finished, "100명 요청이 60초 안에 끝나지 않았다")
        assertEquals(1, success.get())
        assertEquals(1L, reserved)
        assertEquals(HUNDRED - 1, failures.size)
        assertEquals(
            setOf(EXPECTED_REJECTION),
            failures.toSet(),
            "거절 이유가 '이미 선점' 말고도 있었다. ${elapsedMillis}ms",
        )
    }

    companion object {
        private const val HUNDRED: Int = 100
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val EXPECTED_REJECTION: String = "IllegalArgumentException: 다른 예약이 이미 선점한 슬롯입니다."
    }
}
