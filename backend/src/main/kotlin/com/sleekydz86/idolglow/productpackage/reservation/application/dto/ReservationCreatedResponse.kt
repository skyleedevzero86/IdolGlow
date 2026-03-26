package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ReservationCreatedResponse(
    @field:Schema(description = "Reservation id", example = "1")
    val id: Long,
    @field:Schema(description = "Reservation status", example = "PENDING")
    val status: ReservationStatus,
    @field:Schema(description = "Reservation expiration time", example = "2026-03-21T12:15:00")
    val expiresAt: LocalDateTime?,
    @field:Schema(description = "Payment information")
    val payment: PaymentResponse,
) {
    companion object {
        fun from(reservation: Reservation, payment: Payment): ReservationCreatedResponse =
            ReservationCreatedResponse(
                id = reservation.id,
                status = reservation.status,
                expiresAt = reservation.expiresAt,
                payment = PaymentResponse.from(payment)
            )
    }
}
