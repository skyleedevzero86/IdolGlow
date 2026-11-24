package com.sleekydz86.idolglow.visit.infrastructure

import com.sleekydz86.idolglow.visit.application.port.out.SiteVisitPort
import com.sleekydz86.idolglow.visit.domain.SiteDailyVisit
import com.sleekydz86.idolglow.visit.domain.SiteVisitor
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SiteVisitAdapter(
    private val visitorRepository: SiteVisitorJpaRepository,
    private val dailyVisitRepository: SiteDailyVisitJpaRepository,
) : SiteVisitPort {
    override fun findVisitor(visitorId: String): SiteVisitor? = visitorRepository.findByIdOrNull(visitorId)

    override fun insertVisitor(visitor: SiteVisitor): SiteVisitor = visitorRepository.saveAndFlush(visitor)

    override fun findDailyVisit(
        visitorId: String,
        visitDate: LocalDate,
    ): SiteDailyVisit? = dailyVisitRepository.findByVisitorIdAndVisitDate(visitorId, visitDate)

    override fun insertDailyVisit(visit: SiteDailyVisit): SiteDailyVisit = dailyVisitRepository.saveAndFlush(visit)

    override fun countVisitorsByDate(
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Long> = toDateCounts(dailyVisitRepository.countByVisitDate(from, to))

    override fun countNewVisitorsByDate(
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, Long> = toDateCounts(visitorRepository.countByFirstVisitDate(from, to))

    override fun countUserVisitDays(
        userId: Long,
        from: LocalDate,
        to: LocalDate,
    ): Long = dailyVisitRepository.countUserVisitDays(userId, from, to)

    override fun deleteDailyVisitsBefore(date: LocalDate): Int = dailyVisitRepository.deleteBefore(date)

    override fun deleteVisitorsLastVisitedBefore(date: LocalDate): Int = visitorRepository.deleteLastVisitedBefore(date)

    private fun toDateCounts(rows: List<Array<Any>>): Map<LocalDate, Long> =
        rows.associate { row -> (row[0] as LocalDate) to (row[1] as Number).toLong() }
}
