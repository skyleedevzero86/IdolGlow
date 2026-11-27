package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.admin.application.InfrastructureProbe
import com.sleekydz86.idolglow.admin.application.dto.AdminInfrastructureStatusResult
import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.servermetric.application.port.out.ServerMetricPort
import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("서버 점검 기록 저장")
class ServerMetricRecorderTest {
    @DisplayName("점검에 실패한 대상은 DOWN 과 빈 응답시간으로 저장하고, 전체 상태와 HTTP 응답 수를 함께 남긴다")
    @Test
    fun savesFailedProbeAsDownWithoutResponseTime() {
        // given
        val infrastructure =
            listOf(
                status("DATABASE", "DOWN", 2000),
                status("MINIO", "UP", 15),
                status("REDIS", "NOT_CONFIGURED", null),
                status("MQ", "NOT_CONFIGURED", null),
            )
        val probe = mockk<InfrastructureProbe>()
        every { probe.probeAll() } returns infrastructure
        every { probe.overallStatus(infrastructure) } returns "DOWN"
        val counter = mockk<HttpResponseCounter>()
        every { counter.takeSinceLast() } returns HttpResponseDelta(40, 3)
        val port = mockk<ServerMetricPort>()
        val saved = slot<ServerMetricSnapshot>()
        every { port.save(capture(saved)) } answers { saved.captured }
        val recorder = ServerMetricRecorder(probe, counter, port, DashboardTime())

        // when
        recorder.record()

        // then
        assertEquals("DOWN", saved.captured.overallStatus)
        assertEquals("DOWN", saved.captured.dbStatus)
        assertNull(saved.captured.dbResponseMs)
        assertEquals("UP", saved.captured.minioStatus)
        assertEquals(15L, saved.captured.minioResponseMs)
        assertEquals(40L, saved.captured.httpRequestCount)
        assertEquals(3L, saved.captured.httpErrorCount)
    }

    private fun status(
        type: String,
        status: String,
        responseMs: Long?,
    ): AdminInfrastructureStatusResult =
        AdminInfrastructureStatusResult(
            type = type,
            label = type,
            status = status,
            message = "점검",
            responseTimeMs = responseMs,
            details = emptyMap(),
        )
}
