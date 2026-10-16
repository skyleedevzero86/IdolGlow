package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import java.time.LocalDateTime

data class TossBillingSubscriptionResponse(
    val id: Long,
    val planCode: String,
    val orderName: String,
    val amount: Long,
    val status: TossBillingSubscriptionStatus,
    val nextBillingAt: LocalDateTime?,
    val lastChargedAt: LocalDateTime?,
    val failCount: Int,
    val retryAt: LocalDateTime?,
    val endsAt: LocalDateTime?,
    val billingMethod: TossBillingMethodResponse,
) {
    companion object {
        fun from(entity: TossBillingSubscription) =
            TossBillingSubscriptionResponse(
                id = entity.id,
                planCode = entity.planCode,
                orderName = entity.orderName,
                amount = entity.amount,
                status = entity.status,
                nextBillingAt = entity.nextBillingAt,
                lastChargedAt = entity.lastChargedAt,
                failCount = entity.failCount,
                retryAt = entity.retryAt,
                endsAt = entity.endsAt,
                billingMethod = TossBillingMethodResponse.from(entity.billingMethod),
            )
    }
}
