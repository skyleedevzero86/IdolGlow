package com.sleekydz86.idolglow.productpackage.reservation.graphql

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class ReservationGraphQlController(
    private val reservationQueryService: ReservationQueryService,
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun reservations(): List<ReservationSummaryGraphQlResponse> =
        reservationQueryService.findReservationsByUser(authenticatedUserIdResolver.resolveRequired())
            .map(ReservationSummaryGraphQlResponse::from)

    @QueryMapping
    fun upcomingReservations(): List<ReservationSummaryGraphQlResponse> =
        reservationQueryService.findUpcomingReservationsByUser(authenticatedUserIdResolver.resolveRequired())
            .map(ReservationSummaryGraphQlResponse::from)

    @QueryMapping
    fun mySlotWaitlist(): List<SlotWaitlistEntryGraphQlResponse> =
        reservationSlotWaitlistService.findMine(authenticatedUserIdResolver.resolveRequired())
            .map(SlotWaitlistEntryGraphQlResponse::from)

    @QueryMapping
    fun reservation(@Argument id: String): ReservationSummaryGraphQlResponse {
        val reservationId = id.toLongOrNull()
            ?: throw IllegalArgumentException("id는 숫자여야 합니다.")

        return ReservationSummaryGraphQlResponse.from(
            reservationQueryService.findReservationDetail(
                reservationId = reservationId,
                userId = authenticatedUserIdResolver.resolveRequired()
            )
        )
    }
}
