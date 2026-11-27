package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import com.sleekydz86.idolglow.servermetric.infrastructure.ServerMetricSnapshotJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("서버 점검 기록 저장과 보관 정리")
class ServerMetricRecorderPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var recorder: ServerMetricRecorder

    @Autowired private lateinit var repository: ServerMetricSnapshotJpaRepository

    @Autowired private lateinit var dashboardTime: DashboardTime

    private val now: LocalDateTime = LocalDateTime.of(2033, 1, 10, 9, 0)

    @BeforeEach
    fun setUp() {
        dashboardTime.clock = Clock.fixed(now.atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
        dashboardTime.storageZone = IdolGlowDateTimes.ZONE
    }

    @AfterEach
    fun restore() {
        dashboardTime.clock = Clock.system(IdolGlowDateTimes.ZONE)
        dashboardTime.storageZone = ZoneId.systemDefault()
    }

    @DisplayName("실제 DB 점검으로 한 줄을 저장한다. DB 는 UP 과 응답시간, 설정 없는 MinIO 는 NOT_CONFIGURED 다")
    @Test
    fun recordsRealProbe() {
        // given
        val before = repository.count()

        // when
        val saved = recorder.record()

        // then
        assertEquals(before + 1, repository.count())
        assertEquals(now, saved.measuredAt)
        assertEquals("UP", saved.overallStatus)
        assertEquals("UP", saved.dbStatus)
        assertTrue((saved.dbResponseMs ?: -1L) >= 0L)
        assertEquals("NOT_CONFIGURED", saved.minioStatus)
    }

    @DisplayName("보관 정리는 지금−168시간과 같거나 앞선 기록을 지우고 1초 뒤 기록은 남긴다")
    @Test
    fun purgesSnapshotsOlderThanSevenDays() {
        // given
        val boundary = repository.save(snapshot(now.minusHours(168)))
        val kept = repository.save(snapshot(now.minusHours(168).plusSeconds(1)))

        // when
        recorder.purgeOld()

        // then
        assertEquals(false, repository.existsById(boundary.id))
        assertEquals(true, repository.existsById(kept.id))
    }

    private fun snapshot(at: LocalDateTime): ServerMetricSnapshot =
        ServerMetricSnapshot(
            measuredAt = at,
            overallStatus = "UP",
            dbStatus = "UP",
            dbResponseMs = 3,
            minioStatus = "NOT_CONFIGURED",
            minioResponseMs = null,
            httpRequestCount = 0,
            httpErrorCount = 0,
        )
}
