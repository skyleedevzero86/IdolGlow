package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TossBillingSubscriptionJpaRepository : JpaRepository<TossBillingSubscription, Long> {
    fun findByUserIdAndPlanCode(
        userId: Long,
        planCode: String,
    ): TossBillingSubscription?

    fun findByUserIdOrderByIdDesc(userId: Long): List<TossBillingSubscription>

    @EntityGraph(attributePaths = ["billingMethod"])
    fun findWithBillingMethodById(id: Long): TossBillingSubscription?

    @Query(
        """
        SELECT s.id FROM TossBillingSubscription s
        WHERE s.status = :active
          AND (
            (s.retryAt IS NOT NULL AND s.retryAt <= :now)
            OR (s.retryAt IS NULL AND s.nextBillingAt IS NOT NULL AND s.nextBillingAt <= :now)
          )
        """,
    )
    fun findDueSubscriptionIds(
        @Param("active") active: TossBillingSubscriptionStatus,
        @Param("now") now: LocalDateTime,
    ): List<Long>
}
