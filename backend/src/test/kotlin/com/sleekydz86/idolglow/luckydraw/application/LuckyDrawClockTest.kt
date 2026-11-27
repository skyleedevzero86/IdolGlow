package com.sleekydz86.idolglow.luckydraw.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@DisplayName("응모 시계")
class LuckyDrawClockTest {
    @DisplayName("UTC 시계가 11월 30일 15시면 서울 기준 12월 1일 0시로 본다")
    @Test
    fun usesSeoulTimeRegardlessOfClockZone() {
        // given
        val luckyDrawClock = LuckyDrawClock()
        luckyDrawClock.clock = Clock.fixed(Instant.parse("2026-11-30T15:00:00Z"), ZoneOffset.UTC)

        // when
        val now = luckyDrawClock.now()

        // then
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), now)
        assertEquals(LocalDate.of(2026, 12, 1), luckyDrawClock.today())
    }

    @DisplayName("서울 12월 1일 0시 1초 전은 아직 11월 30일이다")
    @Test
    fun beforeSeoulMidnightIsPreviousDay() {
        // given
        val luckyDrawClock = LuckyDrawClock()
        luckyDrawClock.clock = Clock.fixed(Instant.parse("2026-11-30T14:59:59Z"), ZoneOffset.UTC)

        // when
        val today = luckyDrawClock.today()

        // then
        assertEquals(LocalDate.of(2026, 11, 30), today)
    }
}
