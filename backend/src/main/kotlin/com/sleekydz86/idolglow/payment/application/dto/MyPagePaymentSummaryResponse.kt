package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MyPagePaymentSummaryResponse(
    val paymentId: Long,
    val reservationId: Long,
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
    val canceledAt: LocalDateTime?,
    val visitDate: LocalDate,
    val visitStartTime: LocalTime,
    val visitEndTime: LocalTime,
    val canCancel: Boolean,
    val cancelDeadlineAt: LocalDateTime?,
    val receiptAvailable: Boolean,
) {
    companion object {
        fun from(
            payment: Payment,
            canCancel: Boolean,
            cancelDeadlineAt: LocalDateTime?,
            receiptAvailable: Boolean,
        ): MyPagePaymentSummaryResponse =
            MyPagePaymentSummaryResponse(
                paymentId = payment.id,
                reservationId = payment.reservation.id,
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
                canceledAt = payment.canceledAt,
                visitDate = payment.reservation.visitDate,
                visitStartTime = payment.reservation.visitStartTime,
                visitEndTime = payment.reservation.visitEndTime,
                canCancel = canCancel,
                cancelDeadlineAt = cancelDeadlineAt,
                receiptAvailable = receiptAvailable,
            )
    }
}
