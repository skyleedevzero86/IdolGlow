package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.servermetric.application.port.out.ServerMetricPort
import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime

@DisplayName("서버 상태 추이 계산")
class ServerMetricQueryServiceTest {
    private val now: LocalDateTime = LocalDateTime.of(2031, 5, 1, 12, 0)

    @DisplayName("가용성은 UP 줄 비율, 성공률은 500 미만 응답 비율, 평균 응답시간은 DB 응답이 있는 줄 평균이고 모두 반올림한다")
    @Test
    fun calculatesRatesWithRounding() {
        // given
        val service =
            service(
                listOf(
                    snapshot("UP", 10, 1000, 1),
                    snapshot("UP", 11, 1000, 0),
                    snapshot("DOWN", null, 999, 1),
                ),
            )

        // when
        val trend = service.trend(24)

        // then
        assertEquals(66.7, trend.availabilityPercent)
        assertEquals(99.9, trend.successRatePercent)
        assertEquals(11L, trend.averageResponseMs)
        assertEquals(3, trend.points.size)
    }

    @DisplayName("기록이 없으면 가용성·평균이 비고, HTTP 응답이 0건이면 성공률이 빈다")
    @Test
    fun leavesRatesEmptyWithoutData() {
        // given
        val empty = service(emptyList())
        val noTraffic = service(listOf(snapshot("UP", null, 0, 0)))

        // when
        val emptyTrend = empty.trend(24)
        val noTrafficTrend = noTraffic.trend(24)

        // then
        assertNull(emptyTrend.availabilityPercent)
        assertNull(emptyTrend.averageResponseMs)
        assertNull(noTrafficTrend.successRatePercent)
        assertEquals(100.0, noTrafficTrend.availabilityPercent)
    }

    @DisplayName("기간은 지금부터 맞춘 시간만큼 앞에서 시작한다")
    @Test
    fun queriesFromClampedHoursAgo() {
        // given
        val port = mockk<ServerMetricPort>()
        val from = slot<LocalDateTime>()
        every { port.findMeasuredSince(capture(from)) } returns emptyList()
        val service = ServerMetricQueryService(port, fixedTime())

        // when
        val trend = service.trend(500)

        // then
        assertEquals(168, trend.hours)
        assertEquals(now.minusHours(168), from.captured)
    }

    private fun service(snapshots: List<ServerMetricSnapshot>): ServerMetricQueryService {
        val port = mockk<ServerMetricPort>()
        every { port.findMeasuredSince(any()) } returns snapshots
        return ServerMetricQueryService(port, fixedTime())
    }

    private fun fixedTime(): DashboardTime =
        DashboardTime().apply {
            clock = Clock.fixed(now.atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
            storageZone = IdolGlowDateTimes.ZONE
        }

    private fun snapshot(
        overall: String,
        dbMs: Long?,
        requests: Long,
        errors: Long,
    ): ServerMetricSnapshot =
        ServerMetricSnapshot(
            measuredAt = now.minusMinutes(5),
            overallStatus = overall,
            dbStatus = if (dbMs == null) "DOWN" else "UP",
            dbResponseMs = dbMs,
            minioStatus = "NOT_CONFIGURED",
            minioResponseMs = null,
            httpRequestCount = requests,
            httpErrorCount = errors,
        )
}
