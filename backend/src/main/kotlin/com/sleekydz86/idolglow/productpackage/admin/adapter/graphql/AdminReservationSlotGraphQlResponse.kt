package com.sleekydz86.idolglow.productpackage.admin.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse

data class AdminReservationSlotGraphQlResponse(
    val id: String,
    val productId: String,
    val reservationDate: String,
    val startTime: String,
    val endTime: String,
    val booked: Boolean,
    val holdReservationId: String?,
    val holdExpiresAt: String?,
) {
    companion object {
        fun from(response: AdminReservationSlotResponse): AdminReservationSlotGraphQlResponse =
            AdminReservationSlotGraphQlResponse(
                id = response.id.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                reservationDate = requireNotNull(response.reservationDate.asGraphQlValue()),
                startTime = requireNotNull(response.startTime.asGraphQlValue()),
                endTime = requireNotNull(response.endTime.asGraphQlValue()),
                booked = response.booked,
                holdReservationId = response.holdReservationId?.asGraphQlId(),
                holdExpiresAt = response.holdExpiresAt.asGraphQlValue(),
            )
    }
}
