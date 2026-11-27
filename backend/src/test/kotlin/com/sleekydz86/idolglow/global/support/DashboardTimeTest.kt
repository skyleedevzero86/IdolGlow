package com.sleekydz86.idolglow.global.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@DisplayName("대시보드 시간 경계")
class DashboardTimeTest {
    @DisplayName("저장 시간대가 UTC 여도 오늘과 이번 달 시작은 서울 기준이고, 경계 시각은 UTC 로 바꿔 준다")
    @Test
    fun usesSeoulBoundariesWithUtcStorage() {
        // given
        val time = DashboardTime()
        time.clock = Clock.fixed(Instant.parse("2031-02-28T16:30:00Z"), ZoneOffset.UTC)
        time.storageZone = ZoneOffset.UTC

        // when
        val today = time.today()
        val monthStart = time.startOfMonth()
        val dayStart = time.startOf(today)

        // then
        assertEquals(LocalDate.of(2031, 3, 1), today)
        assertEquals(LocalDateTime.of(2031, 2, 28, 15, 0), monthStart)
        assertEquals(LocalDateTime.of(2031, 2, 28, 15, 0), dayStart)
        assertEquals(LocalDateTime.of(2031, 2, 28, 16, 30), time.now())
    }
}
