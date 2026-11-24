package com.sleekydz86.idolglow.visit.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.visit.application.port.out.SiteVisitPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SiteVisitRetentionService(
    private val siteVisitPort: SiteVisitPort,
    private val dashboardTime: DashboardTime,
) {
    @Transactional
    fun purge() {
        val today = dashboardTime.today()
        val dailyDeleted = siteVisitPort.deleteDailyVisitsBefore(today.minusDays(DAILY_VISIT_KEEP_DAYS - 1))
        val visitorsDeleted = siteVisitPort.deleteVisitorsLastVisitedBefore(today.minusDays(VISITOR_KEEP_DAYS - 1))
        log.info("방문 기록 보관 정리 dailyVisits={} visitors={}", dailyDeleted, visitorsDeleted)
    }

    companion object {
        private val log = LoggerFactory.getLogger(SiteVisitRetentionService::class.java)
        const val DAILY_VISIT_KEEP_DAYS: Long = 90L
        const val VISITOR_KEEP_DAYS: Long = 365L
    }
}
