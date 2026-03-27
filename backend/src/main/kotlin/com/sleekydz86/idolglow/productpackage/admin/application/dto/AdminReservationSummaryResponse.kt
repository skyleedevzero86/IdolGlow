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
    @field:Schema(description = "Reservation id", example = "1")
    val reservationId: Long,
    @field:Schema(description = "User id", example = "7")
    val userId: Long,
    @field:Schema(description = "Product id", example = "12")
    val productId: Long,
    @field:Schema(description = "Product name", example = "Hair and makeup package")
    val productName: String,
    @field:Schema(description = "Reservation status", example = "BOOKED")
    val status: ReservationStatus,
    @field:Schema(description = "Total price", example = "300000.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Visit date", example = "2026-03-21")
    val visitDate: LocalDate,
    @field:Schema(description = "Visit start time", example = "09:00:00")
    val visitStartTime: LocalTime,
    @field:Schema(description = "Visit end time", example = "10:00:00")
    val visitEndTime: LocalTime,
    @field:Schema(description = "Expiration time", example = "2026-03-21T12:15:00")
    val expiresAt: LocalDateTime?,
    @field:Schema(description = "Confirmation time", example = "2026-03-21T12:05:00")
    val confirmedAt: LocalDateTime?,
    @field:Schema(description = "Cancellation time", example = "2026-03-21T12:06:00")
    val canceledAt: LocalDateTime?,
    @field:Schema(description = "Cancellation reason", example = "PAYMENT_FAILED")
    val cancelReason: ReservationCancelReason?,
    @field:Schema(description = "Payment reference", example = "pay_mock_20260321120000_1")
    val paymentReference: String?,
    @field:Schema(description = "Payment status", example = "SUCCEEDED")
    val paymentStatus: PaymentStatus?,
    @field:Schema(description = "Payment failure reason", example = "issuer declined")
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
