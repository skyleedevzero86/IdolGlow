package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminPaymentSummaryResponse(
    val paymentId: Long,
    val reservationId: Long,
    val userId: Long,
    val productId: Long,
    val productName: String,
    val provider: PaymentProvider,
    val paymentReference: String,
    val amount: BigDecimal,
    val cancelAmount: BigDecimal,
    val status: PaymentStatus,
    val failureReason: String?,
    val approvedAt: LocalDateTime?,
    val failedAt: LocalDateTime?,
    val expiredAt: LocalDateTime?,
    val visitDate: LocalDate,
    val visitStartTime: LocalTime,
    val visitEndTime: LocalTime,
) {
    companion object {
        fun from(payment: Payment): AdminPaymentSummaryResponse =
            AdminPaymentSummaryResponse(
                paymentId = payment.id,
                reservationId = payment.reservation.id,
                userId = payment.reservation.userId,
                productId = payment.reservation.reservationSlot.product.id,
                productName = payment.reservation.reservationSlot.product.name,
                provider = payment.provider,
                paymentReference = payment.paymentReference,
                amount = payment.amount,
                cancelAmount = payment.cancelAmount,
                status = payment.status,
                failureReason = payment.failureReason,
                approvedAt = payment.approvedAt,
                failedAt = payment.failedAt,
                expiredAt = payment.expiredAt,
                visitDate = payment.reservation.visitDate,
                visitStartTime = payment.reservation.visitStartTime,
                visitEndTime = payment.reservation.visitEndTime,
            )
    }
}
