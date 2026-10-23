package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TossBillingChargeAttemptJpaRepository : JpaRepository<TossBillingChargeAttempt, Long> {
    fun findAllByChargeIdOrderByAttemptNoAsc(chargeId: Long): List<TossBillingChargeAttempt>

    fun findTopByChargeIdOrderByAttemptNoDesc(chargeId: Long): TossBillingChargeAttempt?

    @Query("select a.chargeId from TossBillingChargeAttempt a where a.id = :id")
    fun findChargeIdById(
        @Param("id") id: Long,
    ): Long?

    fun findAllByChargeIdInOrderByChargeIdAscAttemptNoAsc(chargeIds: Collection<Long>): List<TossBillingChargeAttempt>

    @Query(
        """
        select new com.sleekydz86.idolglow.payment.infrastructure.BillingReconcileTarget(
            a.id,
            a.chargeId,
            a.tossOrderId,
            a.createdAt
        )
        from TossBillingChargeAttempt a
        where a.status = :pending
          and a.createdAt <= :createdBefore
          and a.attemptNo = (
              select max(b.attemptNo)
              from TossBillingChargeAttempt b
              where b.chargeId = a.chargeId
          )
          and exists (
              select c.id
              from TossBillingCharge c
              where c.id = a.chargeId
                and c.status = :pending
          )
        order by a.id asc
        """,
    )
    fun findLatestPendingAttempts(
        @Param("pending") pending: TossBillingChargeStatus,
        @Param("createdBefore") createdBefore: LocalDateTime,
        pageable: Pageable,
    ): List<BillingReconcileTarget>
}
