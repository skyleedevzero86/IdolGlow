package com.sleekydz86.idolglow.visit.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.visit.application.dto.DailyVisitorCount
import com.sleekydz86.idolglow.visit.application.dto.VisitorSummary
import com.sleekydz86.idolglow.visit.application.port.out.SiteVisitPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SiteVisitQueryService(
    private val siteVisitPort: SiteVisitPort,
    private val dashboardTime: DashboardTime,
) {
    fun summarize(): VisitorSummary {
        val today = dashboardTime.today()
        val from = today.minusDays(TREND_DAYS - 1)
        val visitors = siteVisitPort.countVisitorsByDate(from, today)
        val newVisitors = siteVisitPort.countNewVisitorsByDate(from, today)
        val daily =
            (0 until TREND_DAYS).map { offset ->
                val date = from.plusDays(offset)
                DailyVisitorCount(date, visitors[date] ?: 0L, newVisitors[date] ?: 0L)
            }
        return VisitorSummary(today = daily.last().visitors, newToday = daily.last().newVisitors, daily = daily)
    }

    fun countUserVisitDays(userId: Long): Long {
        val today = dashboardTime.today()
        return siteVisitPort.countUserVisitDays(userId, today.minusDays(USER_VISIT_DAYS - 1), today)
    }

    companion object {
        const val TREND_DAYS: Long = 7L
        const val USER_VISIT_DAYS: Long = 90L
    }
}
