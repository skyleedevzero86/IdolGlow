package com.sleekydz86.idolglow.visit.infrastructure

import com.sleekydz86.idolglow.visit.domain.SiteDailyVisit
import com.sleekydz86.idolglow.visit.domain.SiteVisitor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SiteVisitorJpaRepository : JpaRepository<SiteVisitor, String> {
    @Query(
        """
        SELECT v.firstVisitDate, count(v)
        FROM SiteVisitor v
        WHERE v.firstVisitDate >= :from AND v.firstVisitDate <= :to
        GROUP BY v.firstVisitDate
        """,
    )
    fun countByFirstVisitDate(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<Array<Any>>

    @Modifying
    @Query("DELETE FROM SiteVisitor v WHERE v.lastVisitDate < :date")
    fun deleteLastVisitedBefore(
        @Param("date") date: LocalDate,
    ): Int
}

interface SiteDailyVisitJpaRepository : JpaRepository<SiteDailyVisit, Long> {
    fun findByVisitorIdAndVisitDate(
        visitorId: String,
        visitDate: LocalDate,
    ): SiteDailyVisit?

    @Query(
        """
        SELECT d.visitDate, count(d)
        FROM SiteDailyVisit d
        WHERE d.visitDate >= :from AND d.visitDate <= :to
        GROUP BY d.visitDate
        """,
    )
    fun countByVisitDate(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<Array<Any>>

    @Query(
        """
        SELECT count(d)
        FROM SiteDailyVisit d
        WHERE d.userId = :userId AND d.visitDate >= :from AND d.visitDate <= :to
        """,
    )
    fun countUserVisitDays(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): Long

    @Modifying
    @Query("DELETE FROM SiteDailyVisit d WHERE d.visitDate < :date")
    fun deleteBefore(
        @Param("date") date: LocalDate,
    ): Int
}
