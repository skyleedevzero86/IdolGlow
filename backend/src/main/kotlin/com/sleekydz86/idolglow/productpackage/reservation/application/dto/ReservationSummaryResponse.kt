package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ReservationSummaryResponse(
    @Schema(description = "Reservation id", example = "1")
    val reservationId: Long,
    @Schema(description = "Reservation status", example = "PENDING")
    val status: ReservationStatus,
    @Schema(description = "Product id", example = "12")
    val productId: Long,
    @Schema(description = "Product name", example = "Hair and makeup package")
    val productName: String,
    @Schema(description = "Product description", example = "Hair and makeup course included")
    val productDescription: String,
    @Schema(description = "Total price", example = "300000.00")
    val totalPrice: BigDecimal,
    @Schema(description = "Visit date", example = "2026-03-21")
    val visitDate: LocalDate,
    @Schema(description = "Visit start time", example = "09:00:00")
    val visitStartTime: LocalTime,
    @Schema(description = "Visit end time", example = "10:00:00")
    val visitEndTime: LocalTime,
    @Schema(description = "Selected attractions", example = "[\"Hair\", \"Makeup\"]")
    val attractions: List<String>,
    @Schema(description = "Expiration time", example = "2026-03-21T12:15:00")
    val expiresAt: LocalDateTime?,
    @Schema(description = "Confirmation time", example = "2026-03-21T12:05:00")
    val confirmedAt: LocalDateTime?,
    @Schema(description = "Cancellation time", example = "2026-03-21T12:06:00")
    val canceledAt: LocalDateTime?,
    @Schema(description = "Cancellation reason", example = "PAYMENT_FAILED")
    val cancelReason: ReservationCancelReason?,
) {
    companion object {
        fun from(
            reservation: Reservation,
            status: ReservationStatus
        ): ReservationSummaryResponse {
            val product = reservation.reservationSlot.product
            val attractions = product.productOptions.map { it.option.name }.distinct()
            return ReservationSummaryResponse(
                reservationId = reservation.id,
                status = status,
                productId = product.id,
                productName = product.name,
                productDescription = product.description,
                totalPrice = reservation.totalPrice,
                visitDate = reservation.visitDate,
                visitStartTime = reservation.visitStartTime,
                visitEndTime = reservation.visitEndTime,
                attractions = attractions,
                expiresAt = reservation.expiresAt,
                confirmedAt = reservation.confirmedAt,
                canceledAt = reservation.canceledAt,
                cancelReason = reservation.cancelReason
            )
        }
    }
}
