package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import java.time.LocalDateTime

data class TossBillingChargeResponse(
    val id: Long,
    val orderId: String,
    val orderName: String,
    val amount: Long,
    val paymentKey: String?,
    val status: TossBillingChargeStatus,
    val failMessage: String?,
    val approvedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: TossBillingCharge) =
            TossBillingChargeResponse(
                id = entity.id,
                orderId = entity.orderId,
                orderName = entity.orderName,
                amount = entity.amount,
                paymentKey = entity.paymentKey,
                status = entity.status,
                failMessage = entity.failMessage,
                approvedAt = entity.approvedAt,
            )
    }
}
