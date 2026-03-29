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
    val amount: String,
    val status: String,
    val approvedAt: String?,
    val failedAt: String?,
    val expiredAt: String?,
    val failureReason: String?,
) {
    companion object {
        fun from(response: ReservationCreatedResponse): PaymentGraphQlResponse =
            PaymentGraphQlResponse(
                paymentId = response.payment.paymentId.asGraphQlId(),
                reservationId = response.payment.reservationId.asGraphQlId(),
                provider = response.payment.provider.name,
                paymentReference = response.payment.paymentReference,
                amount = response.payment.amount.stripTrailingZeros().toPlainString(),
                status = response.payment.status.name,
                approvedAt = response.payment.approvedAt.asGraphQlValue(),
                failedAt = response.payment.failedAt.asGraphQlValue(),
                expiredAt = response.payment.expiredAt.asGraphQlValue(),
                failureReason = response.payment.failureReason
            )

        fun from(payment: Payment): PaymentGraphQlResponse =
            PaymentGraphQlResponse(
                paymentId = payment.id.asGraphQlId(),
                reservationId = payment.reservation.id.asGraphQlId(),
                provider = payment.provider.name,
                paymentReference = payment.paymentReference,
                amount = payment.amount.stripTrailingZeros().toPlainString(),
                status = payment.status.name,
                approvedAt = payment.approvedAt.asGraphQlValue(),
                failedAt = payment.failedAt.asGraphQlValue(),
                expiredAt = payment.expiredAt.asGraphQlValue(),
                failureReason = payment.failureReason
            )
    }
}
