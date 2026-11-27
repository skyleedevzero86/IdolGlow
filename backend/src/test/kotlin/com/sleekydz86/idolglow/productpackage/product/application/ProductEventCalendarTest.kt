package com.sleekydz86.idolglow.productpackage.product.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@DisplayName("이벤트 기준 날짜")
class ProductEventCalendarTest {
    @DisplayName("UTC 시계가 11월 30일 15시여도 서울 날짜 12월 1일로 본다")
    @Test
    fun usesSeoulDateRegardlessOfClockZone() {
        // given
        val calendar = ProductEventCalendar()
        calendar.clock = Clock.fixed(Instant.parse("2026-11-30T15:00:00Z"), ZoneOffset.UTC)

        // when
        val today = calendar.today()

        // then
        assertEquals(LocalDate.of(2026, 12, 1), today)
    }

    @DisplayName("서울 12월 1일 0시 직전은 아직 11월 30일이다")
    @Test
    fun beforeSeoulMidnightIsPreviousDay() {
        // given
        val calendar = ProductEventCalendar()
        calendar.clock = Clock.fixed(Instant.parse("2026-11-30T14:59:59Z"), ZoneOffset.UTC)

        // when
        val today = calendar.today()

        // then
        assertEquals(LocalDate.of(2026, 11, 30), today)
    }
}
