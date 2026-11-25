package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.visit.application.SiteVisitRetentionService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DashboardScheduler(
    private val serverMetricRecorder: ServerMetricRecorder,
    private val siteVisitRetentionService: SiteVisitRetentionService,
) {
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "serverMetricSnapshot", lockAtMostFor = "PT4M", lockAtLeastFor = "PT1M")
    fun recordServerMetric() {
        serverMetricRecorder.record()
    }

    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "dashboardRetentionCleanup", lockAtMostFor = "PT30M")
    fun purgeExpired() {
        runIsolated("site-visits") { siteVisitRetentionService.purge() }
        runIsolated("server-metrics") { serverMetricRecorder.purgeOld() }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun runIsolated(
        target: String,
        purge: () -> Unit,
    ) {
        try {
            purge()
        } catch (failure: RuntimeException) {
            log.error("대시보드 보관 정리 실패, 다음 정리는 계속합니다 target={}", target, failure)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DashboardScheduler::class.java)
    }
}
