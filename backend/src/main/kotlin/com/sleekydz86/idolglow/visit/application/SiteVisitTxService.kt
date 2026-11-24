package com.sleekydz86.idolglow.visit.application

import com.sleekydz86.idolglow.visit.application.port.out.SiteVisitPort
import com.sleekydz86.idolglow.visit.domain.SiteDailyVisit
import com.sleekydz86.idolglow.visit.domain.SiteVisitor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SiteVisitTxService(
    private val siteVisitPort: SiteVisitPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun touchVisitor(
        visitorId: String,
        date: LocalDate,
    ) {
        val existing = siteVisitPort.findVisitor(visitorId)
        if (existing == null) {
            siteVisitPort.insertVisitor(SiteVisitor.first(visitorId, date))
            return
        }
        existing.visitAgain(date)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun touchDailyVisit(
        visitorId: String,
        date: LocalDate,
        userId: Long?,
        ipAddress: String?,
    ) {
        val existing = siteVisitPort.findDailyVisit(visitorId, date)
        if (existing == null) {
            siteVisitPort.insertDailyVisit(SiteDailyVisit.first(visitorId, date, userId, ipAddress))
            return
        }
        existing.attachUser(userId)
    }
}
