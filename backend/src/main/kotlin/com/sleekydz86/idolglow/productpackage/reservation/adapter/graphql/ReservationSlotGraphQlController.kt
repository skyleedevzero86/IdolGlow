package com.sleekydz86.idolglow.productpackage.reservation.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.adapter.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotQueryService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.OpenReservationSlotResponse
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class ReservationSlotGraphQlController(
    private val reservationSlotQueryService: ReservationSlotQueryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun openReservationSlots(
        @Argument productId: String,
    ): List<OpenReservationSlotGraphQlResponse> =
        reservationSlotQueryService
            .upcomingSlots(productId.toGraphQlIdLong("productId"), authenticatedUserIdResolver.resolveRequired())
            .map(OpenReservationSlotGraphQlResponse::from)
}

data class OpenReservationSlotGraphQlResponse(
    val reservationSlotId: String,
    val reservationDate: String,
    val startTime: String,
    val endTime: String,
    val available: Boolean,
    val memberOpenAt: String,
    val publicOpenAt: String,
    val totalPrice: String,
) {
    companion object {
        fun from(slot: OpenReservationSlotResponse) =
            OpenReservationSlotGraphQlResponse(
                reservationSlotId = slot.reservationSlotId.asGraphQlId(),
                reservationDate = slot.reservationDate.toString(),
                startTime = slot.startTime.toString(),
                endTime = slot.endTime.toString(),
                available = slot.available,
                memberOpenAt = slot.memberOpenAt.toString(),
                publicOpenAt = slot.publicOpenAt.toString(),
                totalPrice = slot.totalPrice.asGraphQlNumber(),
            )
    }
}
