package com.sleekydz86.idolglow.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse

data class PaymentGraphQlResponse(
    val paymentId: String,
    val reservationId: String,
    val provider: String,
    val paymentReference: String,
    val paymentNo: String,
    val orderId: String,
    val paymentKey: String?,
    val amount: String,
    val status: String,
    val externalStatus: String?,
    val approvedAt: String?,
    val failedAt: String?,
    val expiredAt: String?,
    val failureReason: String?,
    val failCode: String?,
    val cancelAmount: String,
) {
    companion object {
        fun from(response: ReservationCreatedResponse): PaymentGraphQlResponse =
            from(response.payment)

        fun from(response: com.sleekydz86.idolglow.payment.application.dto.PaymentResponse): PaymentGraphQlResponse =
            PaymentGraphQlResponse(
                paymentId = response.paymentId.asGraphQlId(),
                reservationId = response.reservationId.asGraphQlId(),
                provider = response.provider.name,
                paymentReference = response.paymentReference,
                paymentNo = response.paymentNo,
                orderId = response.orderId,
                paymentKey = response.paymentKey,
                amount = response.amount.stripTrailingZeros().toPlainString(),
                status = response.status.name,
                externalStatus = response.externalStatus,
                approvedAt = response.approvedAt.asGraphQlValue(),
                failedAt = response.failedAt.asGraphQlValue(),
                expiredAt = response.expiredAt.asGraphQlValue(),
                failureReason = response.failureReason,
                failCode = response.failCode,
                cancelAmount = response.cancelAmount.stripTrailingZeros().toPlainString(),
            )

        fun from(payment: Payment): PaymentGraphQlResponse =
            PaymentGraphQlResponse(
                paymentId = payment.id.asGraphQlId(),
                reservationId = payment.reservation.id.asGraphQlId(),
                provider = payment.provider.name,
                paymentReference = payment.paymentReference,
                paymentNo = payment.paymentNo,
                orderId = payment.orderId,
                paymentKey = payment.paymentKey,
                amount = payment.amount.stripTrailingZeros().toPlainString(),
                status = payment.status.name,
                externalStatus = payment.externalStatus,
                approvedAt = payment.approvedAt.asGraphQlValue(),
                failedAt = payment.failedAt.asGraphQlValue(),
                expiredAt = payment.expiredAt.asGraphQlValue(),
                failureReason = payment.failureReason,
                failCode = payment.failCode,
                cancelAmount = payment.cancelAmount.stripTrailingZeros().toPlainString(),
            )
    }
}
