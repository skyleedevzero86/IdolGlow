package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import java.time.LocalDateTime

data class TossBillingChargeAttemptResponse(
    val id: Long,
    val billingMethodId: Long,
    val cardCompany: String?,
    val cardNumber: String?,
    val attemptNo: Int,
    val tossOrderId: String,
    val paymentKey: String?,
    val status: TossBillingChargeStatus,
    val failMessage: String?,
    val approvedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: TossBillingChargeAttempt) =
            TossBillingChargeAttemptResponse(
                id = entity.id,
                billingMethodId = entity.billingMethod.id,
                cardCompany = entity.billingMethod.cardCompany,
                cardNumber = entity.billingMethod.cardNumber,
                attemptNo = entity.attemptNo,
                tossOrderId = entity.tossOrderId,
                paymentKey = entity.paymentKey,
                status = entity.status,
                failMessage = entity.failMessage,
                approvedAt = entity.approvedAt,
            )
    }
}
