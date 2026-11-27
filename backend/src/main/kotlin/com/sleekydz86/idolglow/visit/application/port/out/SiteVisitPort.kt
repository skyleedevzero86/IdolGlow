package com.sleekydz86.idolglow.visit.application.port.out

import com.sleekydz86.idolglow.visit.domain.SiteDailyVisit
import com.sleekydz86.idolglow.visit.domain.SiteVisitor
import java.time.LocalDate

interface SiteVisitPort {
    fun findVisitor(visitorId: String): SiteVisitor?

    fun insertVisitor(visitor: SiteVisitor): SiteVisitor

    fun findDailyVisit(
        visitorId: String,
        visitDate: LocalDate,
    ): SiteDailyVisit?

    fun insertDailyVisit(visit: SiteDailyVisit): SiteDailyVisit

    fun countVisitorsByDate(
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Long>

    fun countNewVisitorsByDate(
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Long>

    fun countUserVisitDays(
        userId: Long,
        from: LocalDate,
        to: LocalDate,
    ): Long

    fun deleteDailyVisitsBefore(date: LocalDate): Int

    fun deleteVisitorsLastVisitedBefore(date: LocalDate): Int
}
