package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.visit.application.SiteVisitRetentionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("대시보드 보관 정리 스케줄러")
class DashboardSchedulerTest {
    @DisplayName("방문 기록 정리가 실패해도 서버 점검 기록 정리는 한다")
    @Test
    fun purgesServerMetricsEvenWhenVisitPurgeFails() {
        // given
        val visitRetention = mockk<SiteVisitRetentionService>()
        every { visitRetention.purge() } throws IllegalStateException("잠금 대기 시간 초과")
        val recorder = mockk<ServerMetricRecorder>()
        every { recorder.purgeOld() } returns 3
        val scheduler = DashboardScheduler(recorder, visitRetention)

        // when
        scheduler.purgeExpired()

        // then
        verify(exactly = 1) { recorder.purgeOld() }
    }

    @DisplayName("서버 점검 기록 정리가 실패해도 방문 기록 정리는 한다")
    @Test
    fun purgesVisitsEvenWhenMetricPurgeFails() {
        // given
        val visitRetention = mockk<SiteVisitRetentionService>(relaxed = true)
        val recorder = mockk<ServerMetricRecorder>()
        every { recorder.purgeOld() } throws IllegalStateException("잠금 대기 시간 초과")
        val scheduler = DashboardScheduler(recorder, visitRetention)

        // when
        scheduler.purgeExpired()

        // then
        verify(exactly = 1) { visitRetention.purge() }
    }
}
