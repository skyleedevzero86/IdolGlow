package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminReservationSlotResponse(
    @field:Schema(description = "Slot id", example = "1")
    val id: Long,
    @field:Schema(description = "Product id", example = "1")
    val productId: Long,
    @field:Schema(description = "Reservation date", example = "2026-03-21")
    val reservationDate: LocalDate,
    @field:Schema(description = "Start time", example = "09:00:00")
    val startTime: LocalTime,
    @field:Schema(description = "End time", example = "10:00:00")
    val endTime: LocalTime,
    @field:Schema(description = "Booked state", example = "false")
    val booked: Boolean,
    @field:Schema(description = "Held reservation id", example = "21")
    val holdReservationId: Long?,
    @field:Schema(description = "Hold expiration time", example = "2026-03-21T12:15:00")
    val holdExpiresAt: LocalDateTime?,
) {
    companion object {
        fun from(slot: ReservationSlot): AdminReservationSlotResponse =
            AdminReservationSlotResponse(
                id = slot.id,
                productId = slot.product.id,
                reservationDate = slot.reservationDate,
                startTime = slot.startTime,
                endTime = slot.endTime,
                booked = slot.isStatusBooked,
                holdReservationId = slot.holdReservationId,
                holdExpiresAt = slot.holdExpiresAt
            )
    }
}
