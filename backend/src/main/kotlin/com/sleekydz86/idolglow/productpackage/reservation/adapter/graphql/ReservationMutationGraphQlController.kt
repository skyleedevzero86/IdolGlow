package com.sleekydz86.idolglow.productpackage.reservation.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.toGraphQlBigDecimal
import com.sleekydz86.idolglow.global.adapter.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class ReservationMutationGraphQlController(
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
    private val reservationCommandService: ReservationCommandService,
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
) {
    @MutationMapping
    fun createReservation(
        @Argument productId: String,
        @Argument input: CreateReservationGraphQlInput,
    ): ReservationCreatedGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val created =
            reservationCommandService.createReservation(
                CreateReservationCommand(
                    productId = productId.toGraphQlIdLong("productId"),
                    reservationSlotId = input.reservationSlotId.toGraphQlIdLong("reservationSlotId"),
                    userId = userId,
                    totalPrice = input.totalPrice.toGraphQlBigDecimal("totalPrice"),
                ),
            )
        return ReservationCreatedGraphQlResponse.from(created)
    }

    @MutationMapping
    fun registerSlotWaitlist(
        @Argument productId: String,
        @Argument reservationSlotId: String,
    ): SlotWaitlistEntryGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val entry =
            reservationSlotWaitlistService.register(
                userId,
                productId.toGraphQlIdLong("productId"),
                reservationSlotId.toGraphQlIdLong("reservationSlotId"),
            )
        return SlotWaitlistEntryGraphQlResponse.from(entry)
    }

    @MutationMapping
    fun unregisterSlotWaitlist(
        @Argument productId: String,
        @Argument reservationSlotId: String,
    ): Boolean {
        val userId = authenticatedUserIdResolver.resolveRequired()
        reservationSlotWaitlistService.unregister(
            userId,
            productId.toGraphQlIdLong("productId"),
            reservationSlotId.toGraphQlIdLong("reservationSlotId"),
        )
        return true
    }

    @MutationMapping
    fun cancelReservation(
        @Argument reservationId: String,
    ): ReservationSummaryGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val reservation =
            reservationCommandService.cancelReservationByUser(
                reservationId = reservationId.toGraphQlIdLong("reservationId"),
                userId = userId,
            )
        return ReservationSummaryGraphQlResponse.from(
            ReservationSummaryResponse.from(
                reservation,
                reservation.resolveStatus(),
            ),
        )
    }
}
