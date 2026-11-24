package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.application.port.out.BillingAmount
import com.sleekydz86.idolglow.payment.application.port.out.RevenueQueryPort
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class RevenueQueryAdapter(
    private val entityManager: EntityManager,
) : RevenueQueryPort {
    override fun sumNetApprovedBetween(
        from: LocalDateTime,
        toExclusive: LocalDateTime,
    ): BigDecimal =
        entityManager
            .createQuery(
                "SELECT coalesce(sum(p.amount - p.cancelAmount), 0) FROM Payment p " +
                    "WHERE p.approvedAt >= :from AND p.approvedAt < :toExclusive",
                BigDecimal::class.java,
            ).setParameter("from", from)
            .setParameter("toExclusive", toExclusive)
            .singleResult ?: BigDecimal.ZERO

    override fun findActiveBillingAmounts(): List<BillingAmount> =
        entityManager
            .createQuery(
                "SELECT new com.sleekydz86.idolglow.payment.application.port.out.BillingAmount(s.amount, s.cycleMonths) " +
                    "FROM TossBillingSubscription s WHERE s.status = :status",
                BillingAmount::class.java,
            ).setParameter("status", TossBillingSubscriptionStatus.ACTIVE)
            .resultList
}
