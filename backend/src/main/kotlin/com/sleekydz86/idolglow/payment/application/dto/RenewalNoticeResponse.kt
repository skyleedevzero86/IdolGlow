package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.BillingCycle
import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import java.time.LocalDateTime

data class RenewalNoticeResponse(
    val periodEnd: LocalDateTime,
    val status: TossBillingSubscriptionStatus,
    val autoRenew: Boolean,
    val cycleMonths: Int,
    val cycleLabel: String,
    val amount: Long,
    val orderName: String,
    val decision: RenewalDecision?,
    val expired: Boolean,
) {
    companion object {
        fun of(
            notice: TossBillingRenewalNotice,
            subscription: TossBillingSubscription,
            now: LocalDateTime,
        ) = RenewalNoticeResponse(
            periodEnd = notice.periodEnd,
            status = subscription.status,
            autoRenew = subscription.autoRenew,
            cycleMonths = subscription.cycleMonths,
            cycleLabel = BillingCycle.ofMonths(subscription.cycleMonths).label,
            amount = subscription.amount,
            orderName = subscription.orderName,
            decision = notice.decision,
            expired = notice.isExpired(now),
        )
    }
}
