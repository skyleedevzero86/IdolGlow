package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChange
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChangeStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface TossBillingPlanChangeJpaRepository : JpaRepository<TossBillingPlanChange, Long> {
    fun findByChargeId(chargeId: Long): TossBillingPlanChange?

    fun findBySubscriptionIdAndPeriodEndAtAndToCycleMonths(
        subscriptionId: Long,
        periodEndAt: LocalDateTime,
        toCycleMonths: Int,
    ): TossBillingPlanChange?

    fun existsBySubscriptionIdAndStatus(
        subscriptionId: Long,
        status: TossBillingPlanChangeStatus,
    ): Boolean
}
