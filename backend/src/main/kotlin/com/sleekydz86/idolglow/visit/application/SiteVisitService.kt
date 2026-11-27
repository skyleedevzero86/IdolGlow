package com.sleekydz86.idolglow.visit.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.visit.domain.VisitorId
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class SiteVisitService(
    private val siteVisitTxService: SiteVisitTxService,
    private val dashboardTime: DashboardTime,
) {
    fun resolveVisitorId(rawCookie: String?): String = VisitorId.normalizeOrNull(rawCookie) ?: VisitorId.newId()

    fun record(
        visitorId: String,
        userId: Long?,
        ipAddress: String?,
    ) {
        val today = dashboardTime.today()
        retryOnceOnConflict(visitorId) { siteVisitTxService.touchVisitor(visitorId, today) }
        retryOnceOnConflict(visitorId) { siteVisitTxService.touchDailyVisit(visitorId, today, userId, ipAddress) }
    }

    private fun retryOnceOnConflict(
        visitorId: String,
        action: () -> Unit,
    ) {
        try {
            action()
        } catch (conflict: DataIntegrityViolationException) {
            log.debug("방문 기록 동시 저장 충돌, 다시 읽어 이어 갑니다 visitor={}", visitorId.take(LOG_ID_LENGTH), conflict)
            action()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SiteVisitService::class.java)
        const val LOG_ID_LENGTH: Int = 8
    }
}
