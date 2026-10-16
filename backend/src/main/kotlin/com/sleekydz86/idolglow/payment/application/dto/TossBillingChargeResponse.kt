package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import java.time.LocalDateTime

data class TossBillingChargeResponse(
    val id: Long,
    val orderId: String,
    val orderName: String,
    val amount: Long,
    val billingMethodId: Long,
    val paymentKey: String?,
    val status: TossBillingChargeStatus,
    val failMessage: String?,
    val approvedAt: LocalDateTime?,
    val attempts: List<TossBillingChargeAttemptResponse> = emptyList(),
) {
    companion object {
        fun from(
            entity: TossBillingCharge,
            attempts: List<TossBillingChargeAttempt> = emptyList(),
        ) = TossBillingChargeResponse(
            id = entity.id,
            orderId = entity.orderId,
            orderName = entity.orderName,
            amount = entity.amount,
            billingMethodId = entity.billingMethod.id,
            paymentKey = entity.paymentKey,
            status = entity.status,
            failMessage = entity.failMessage,
            approvedAt = entity.approvedAt,
            attempts = attempts.map(TossBillingChargeAttemptResponse::from),
        )
    }
}
