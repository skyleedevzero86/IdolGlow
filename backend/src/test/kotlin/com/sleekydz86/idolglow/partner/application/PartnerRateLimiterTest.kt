package com.sleekydz86.idolglow.partner.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("파트너 분당 호출 제한")
class PartnerRateLimiterTest {
    private val limiter = PartnerRateLimiter()

    @DisplayName("한도를 넘으면 막고 다음 분까지 남은 초를 알려 준다")
    @Test
    fun rejectsOverLimitWithRetryAfter() {
        // given
        limiter.clock = 고정("2026-11-21T00:00:45Z")
        repeat(2) { limiter.acquire(partnerId = 1, limitPerMinute = 2) }

        // when
        val decision = limiter.acquire(partnerId = 1, limitPerMinute = 2)

        // then
        assertFalse(decision.allowed)
        assertEquals(15, decision.retryAfterSeconds)
    }

    @DisplayName("다음 분이 되면 다시 허용한다")
    @Test
    fun resetsOnNextMinute() {
        // given
        limiter.clock = 고정("2026-11-21T00:00:59Z")
        repeat(3) { limiter.acquire(partnerId = 1, limitPerMinute = 2) }
        limiter.clock = 고정("2026-11-21T00:01:00Z")

        // when
        val decision = limiter.acquire(partnerId = 1, limitPerMinute = 2)

        // then
        assertTrue(decision.allowed)
    }

    @DisplayName("파트너마다 따로 센다")
    @Test
    fun countsPerPartner() {
        // given
        limiter.clock = 고정("2026-11-21T00:00:10Z")
        repeat(2) { limiter.acquire(partnerId = 1, limitPerMinute = 2) }

        // when
        val other = limiter.acquire(partnerId = 2, limitPerMinute = 2)

        // then
        assertTrue(other.allowed)
    }

    private fun 고정(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}
