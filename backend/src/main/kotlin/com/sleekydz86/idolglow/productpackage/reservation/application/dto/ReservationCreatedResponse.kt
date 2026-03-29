package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import com.sleekydz86.idolglow.payment.application.dto.PaymentResponse
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "예약 생성 응답 DTO")
data class ReservationCreatedResponse(
    @field:Schema(description = "예약 ID", example = "1")
    val id: Long,
    @field:Schema(description = "예약 상태", example = "PENDING")
    val status: ReservationStatus,
    @field:Schema(description = "예약 만료 시각", example = "2026-03-21T12:15:00")
    val expiresAt: LocalDateTime?,
    @field:Schema(description = "결제 정보")
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
