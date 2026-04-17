package com.sleekydz86.idolglow.productpackage.admin.application.dto

import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminReservationSummaryResponse(
    @field:Schema(description = "예약 ID", example = "1")
    val reservationId: Long,
    @field:Schema(description = "회원 ID", example = "7")
    val userId: Long,
    @field:Schema(description = "상품 ID", example = "12")
    val productId: Long,
    @field:Schema(description = "상품명", example = "헤어·메이크업 패키지")
    val productName: String,
    @field:Schema(description = "예약 상태", example = "BOOKED")
    val status: ReservationStatus,
    @field:Schema(description = "총 결제 금액", example = "300000.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "방문일", example = "2026-03-21")
    val visitDate: LocalDate,
    @field:Schema(description = "방문 시작 시각", example = "09:00:00")
    val visitStartTime: LocalTime,
    @field:Schema(description = "방문 종료 시각", example = "10:00:00")
    val visitEndTime: LocalTime,
    @field:Schema(description = "결제·홀드 만료 시각", example = "2026-03-21T12:15:00")
    val expiresAt: LocalDateTime?,
    @field:Schema(description = "확정 시각", example = "2026-03-21T12:05:00")
    val confirmedAt: LocalDateTime?,
    @field:Schema(description = "취소 시각", example = "2026-03-21T12:06:00")
    val canceledAt: LocalDateTime?,
    @field:Schema(description = "취소 사유", example = "PAYMENT_FAILED")
    val cancelReason: ReservationCancelReason?,
    @field:Schema(description = "결제 참조 번호", example = "pay_mock_20260321120000_1")
    val paymentReference: String?,
    @field:Schema(description = "결제 상태", example = "SUCCEEDED")
    val paymentStatus: PaymentStatus?,
    @field:Schema(description = "결제 실패 사유", example = "카드사 거절")
    val paymentFailureReason: String?,
) {
    companion object {
        fun from(
            reservation: Reservation,
            payment: AdminReservationPaymentProjection?,
        ): AdminReservationSummaryResponse =
            AdminReservationSummaryResponse(
                reservationId = reservation.id,
                userId = reservation.userId,
                productId = reservation.reservationSlot.product.id,
                productName = reservation.reservationSlot.product.name,
                status = reservation.resolveStatus(),
                totalPrice = reservation.totalPrice,
                visitDate = reservation.visitDate,
                visitStartTime = reservation.visitStartTime,
                visitEndTime = reservation.visitEndTime,
                expiresAt = reservation.expiresAt,
                confirmedAt = reservation.confirmedAt,
                canceledAt = reservation.canceledAt,
                cancelReason = reservation.cancelReason,
                paymentReference = payment?.paymentReference,
                paymentStatus = payment?.status,
                paymentFailureReason = payment?.failureReason
            )
    }
}

data class AdminReservationPaymentProjection(
    val reservationId: Long,
    val paymentReference: String,
    val status: PaymentStatus,
    val failureReason: String?,
)
