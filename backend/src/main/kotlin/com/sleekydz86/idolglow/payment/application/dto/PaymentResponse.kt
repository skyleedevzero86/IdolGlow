package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "결제 응답 DTO")
data class PaymentResponse(
    @field:Schema(description = "결제 ID", example = "1")
    val paymentId: Long,
    @field:Schema(description = "예약 ID", example = "1")
    val reservationId: Long,
    @field:Schema(description = "결제 수단 제공자", example = "MOCK")
    val provider: PaymentProvider,
    @field:Schema(description = "결제 참조값", example = "pay_mock_20260321120000_1")
    val paymentReference: String,
    @field:Schema(description = "결제 금액", example = "300000.00")
    val amount: BigDecimal,
    @field:Schema(description = "결제 상태", example = "PENDING")
    val status: PaymentStatus,
    @field:Schema(description = "승인 시각", example = "2026-03-21T12:00:00")
    val approvedAt: LocalDateTime?,
    @field:Schema(description = "실패 시각", example = "2026-03-21T12:00:00")
    val failedAt: LocalDateTime?,
    @field:Schema(description = "만료 시각", example = "2026-03-21T12:15:00")
    val expiredAt: LocalDateTime?,
    @field:Schema(description = "실패 사유", example = "issuer declined")
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
