package com.sleekydz86.idolglow.visit.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "site_visitors")
class SiteVisitor(
    @Id
    @Column(name = "visitor_id", nullable = false, length = VisitorId.LENGTH)
    val visitorId: String,
    @Column(name = "first_visit_date", nullable = false, updatable = false)
    val firstVisitDate: LocalDate,
    lastVisitDate: LocalDate,
) : BaseEntity() {
    @Column(name = "last_visit_date", nullable = false)
    var lastVisitDate: LocalDate = lastVisitDate
        protected set

    fun visitAgain(date: LocalDate) {
        if (date.isAfter(lastVisitDate)) {
            lastVisitDate = date
        }
    }

    companion object {
        fun first(
            visitorId: String,
            date: LocalDate,
        ): SiteVisitor = SiteVisitor(visitorId = visitorId, firstVisitDate = date, lastVisitDate = date)
    }
}
