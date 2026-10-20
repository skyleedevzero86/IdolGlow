package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
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

        start.countDown()
        done.await(30, TimeUnit.SECONDS)
        pool.shutdownNow()

        val reserved =
            jdbcTemplate.queryForObject(
                "select count(*) from reservations where reservation_slot_id = ?",
                Long::class.java,
                slot.slotId,
            )
        assertEquals(1, success.get())
        assertEquals(1L, reserved)
    }
}
