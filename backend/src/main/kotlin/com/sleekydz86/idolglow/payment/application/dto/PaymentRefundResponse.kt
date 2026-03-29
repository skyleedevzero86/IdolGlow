package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
import java.math.BigDecimal

data class PaymentRefundResponse(
    val refundId: Long,
    val paymentId: Long,
    val reservationId: Long,
    val cancelAmount: BigDecimal,
    val cancelReason: String,
    val status: PaymentRefundStatus,
    val requestedBy: RefundRequestedBy,
    val externalTransactionKey: String?,
    val failCode: String?,
    val failMessage: String?,
) {
    companion object {
        fun from(entity: PaymentRefund): PaymentRefundResponse =
            PaymentRefundResponse(
                refundId = entity.id,
                paymentId = entity.payment.id,
                reservationId = entity.reservation.id,
                cancelAmount = entity.cancelAmount,
                cancelReason = entity.cancelReason,
                status = entity.status,
                requestedBy = entity.requestedBy,
                externalTransactionKey = entity.externalTransactionKey,
                failCode = entity.failCode,
                failMessage = entity.failMessage,
            )
    }
}
