package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotWaitlistEntry
import java.time.LocalDate
import java.time.LocalTime

data class SlotWaitlistEntryResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val reservationSlotId: Long,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
) {
    companion object {
        fun from(entry: ReservationSlotWaitlistEntry): SlotWaitlistEntryResponse {
            val slot = entry.reservationSlot
            val product = slot.product
            return SlotWaitlistEntryResponse(
                id = entry.id,
                productId = product.id,
                productName = product.name,
                reservationSlotId = slot.id,
                reservationDate = slot.reservationDate,
                startTime = slot.startTime,
                endTime = slot.endTime,
            )
        }
    }
}
