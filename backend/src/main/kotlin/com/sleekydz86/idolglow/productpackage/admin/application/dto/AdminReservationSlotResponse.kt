package com.sleekydz86.idolglow.productpackage.admin.application.dto

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminReservationSlotResponse(
    @field:Schema(description = "슬롯 ID", example = "1")
    val id: Long,
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    val productName: String,
    @field:Schema(description = "예약일", example = "2026-03-21")
    val reservationDate: LocalDate,
    @field:Schema(description = "시작 시각", example = "09:00:00")
    val startTime: LocalTime,
    @field:Schema(description = "종료 시각", example = "10:00:00")
    val endTime: LocalTime,
    @field:Schema(description = "예약 확정 여부", example = "false")
    val booked: Boolean,
    @field:Schema(description = "홀드 중인 예약 ID", example = "21")
    val holdReservationId: Long?,
    @field:Schema(description = "홀드 만료 시각", example = "2026-03-21T12:15:00")
    val holdExpiresAt: LocalDateTime?,
    val adminNote: String?,
) {
    companion object {
        fun from(slot: ReservationSlot): AdminReservationSlotResponse =
            AdminReservationSlotResponse(
                id = slot.id,
                productId = slot.product.id,
                productName = slot.product.name,
                reservationDate = slot.reservationDate,
                startTime = slot.startTime,
                endTime = slot.endTime,
                booked = slot.isStatusBooked,
                holdReservationId = slot.holdReservationId,
                holdExpiresAt = slot.holdExpiresAt,
                adminNote = slot.adminNote,
            )
    }
}
