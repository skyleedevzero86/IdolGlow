package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface TossBillingRenewalNoticeJpaRepository : JpaRepository<TossBillingRenewalNotice, Long> {
    fun findByTokenHash(tokenHash: String): TossBillingRenewalNotice?

    fun existsBySubscriptionIdAndPeriodEnd(
        subscriptionId: Long,
        periodEnd: LocalDateTime,
    ): Boolean
}
