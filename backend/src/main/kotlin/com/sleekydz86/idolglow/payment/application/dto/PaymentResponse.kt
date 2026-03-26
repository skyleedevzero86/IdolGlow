package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentResponse(
    @field:Schema(description = "Payment id", example = "1")
    val paymentId: Long,
    @field:Schema(description = "Reservation id", example = "1")
    val reservationId: Long,
    @field:Schema(description = "Payment provider", example = "MOCK")
    val provider: PaymentProvider,
    @field:Schema(description = "Payment reference", example = "pay_mock_20260321120000_1")
    val paymentReference: String,
    @field:Schema(description = "Payment amount", example = "300000.00")
    val amount: BigDecimal,
    @field:Schema(description = "Payment status", example = "PENDING")
    val status: PaymentStatus,
    @field:Schema(description = "Approved time", example = "2026-03-21T12:00:00")
    val approvedAt: LocalDateTime?,
    @field:Schema(description = "Failed time", example = "2026-03-21T12:00:00")
    val failedAt: LocalDateTime?,
    @field:Schema(description = "Expired time", example = "2026-03-21T12:15:00")
    val expiredAt: LocalDateTime?,
    @field:Schema(description = "Failure reason", example = "issuer declined")
    val failureReason: String?,
) {
    companion object {
        fun from(payment: Payment): PaymentResponse =
            PaymentResponse(
                paymentId = payment.id,
                reservationId = payment.reservation.id,
                provider = payment.provider,
                paymentReference = payment.paymentReference,
                amount = payment.amount,
                status = payment.status,
                approvedAt = payment.approvedAt,
                failedAt = payment.failedAt,
                expiredAt = payment.expiredAt,
                failureReason = payment.failureReason
            )
    }
}
