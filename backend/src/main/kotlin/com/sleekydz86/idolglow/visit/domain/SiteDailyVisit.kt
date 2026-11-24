package com.sleekydz86.idolglow.visit.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "site_daily_visits",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_site_daily_visits_visitor_date", columnNames = ["visitor_id", "visit_date"]),
    ],
)
class SiteDailyVisit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "visitor_id", nullable = false, updatable = false, length = VisitorId.LENGTH)
    val visitorId: String,
    @Column(name = "visit_date", nullable = false, updatable = false)
    val visitDate: LocalDate,
    @Column(name = "ip_address", updatable = false, length = IP_MAX_LENGTH)
    val ipAddress: String?,
    userId: Long?,
) : BaseEntity() {
    @Column(name = "user_id")
    var userId: Long? = userId
        protected set

    fun attachUser(candidate: Long?) {
        if (userId == null && candidate != null) {
            userId = candidate
        }
    }

    companion object {
        const val IP_MAX_LENGTH: Int = 45

        fun first(
            visitorId: String,
            visitDate: LocalDate,
            userId: Long?,
            ipAddress: String?,
        ): SiteDailyVisit =
            SiteDailyVisit(
                visitorId = visitorId,
                visitDate = visitDate,
                ipAddress = ipAddress?.take(IP_MAX_LENGTH)?.takeIf { it.isNotBlank() },
                userId = userId,
            )
    }
}
