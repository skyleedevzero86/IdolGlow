package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import org.springframework.data.jpa.repository.EntityGraph
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TossBillingSubscriptionJpaRepository : JpaRepository<TossBillingSubscription, Long> {
    fun findByUserIdAndPlanCode(
        userId: Long,
        planCode: String,
    ): TossBillingSubscription?

    fun findByUserIdOrderByIdDesc(userId: Long): List<TossBillingSubscription>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select s from TossBillingSubscription s join fetch s.billingMethod " +
            "where s.userId = :userId and s.planCode = :planCode",
    )
    fun findByUserIdAndPlanCodeForUpdate(
        @Param("userId") userId: Long,
        @Param("planCode") planCode: String,
    ): TossBillingSubscription?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TossBillingSubscription s join fetch s.billingMethod where s.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): TossBillingSubscription?

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

    @Query(
        """
        SELECT s.id FROM TossBillingSubscription s
        WHERE s.status IN :statuses
          AND s.nextBillingAt IS NOT NULL
          AND s.nextBillingAt > :now
          AND s.nextBillingAt <= :until
        """,
    )
    fun findRenewalNoticeTargetIds(
        @Param("statuses") statuses: Collection<TossBillingSubscriptionStatus>,
        @Param("now") now: LocalDateTime,
        @Param("until") until: LocalDateTime,
    ): List<Long>
}
