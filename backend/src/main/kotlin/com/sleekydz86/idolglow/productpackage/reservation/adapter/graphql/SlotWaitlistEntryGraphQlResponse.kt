package com.sleekydz86.idolglow.productpackage.reservation.graphql

import com.sleekydz86.idolglow.productpackage.reservation.application.dto.SlotWaitlistEntryResponse

data class SlotWaitlistEntryGraphQlResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val reservationSlotId: String,
    val reservationDate: String,
    val startTime: String,
    val endTime: String,
) {
    companion object {
        fun from(response: SlotWaitlistEntryResponse): SlotWaitlistEntryGraphQlResponse =
            SlotWaitlistEntryGraphQlResponse(
                id = response.id.toString(),
                productId = response.productId.toString(),
                productName = response.productName,
                reservationSlotId = response.reservationSlotId.toString(),
                reservationDate = response.reservationDate.toString(),
                startTime = response.startTime.toString(),
                endTime = response.endTime.toString(),
            )
    }
}
