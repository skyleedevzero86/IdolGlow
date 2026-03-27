package com.sleekydz86.idolglow.productpackage.reservation.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ReservationGraphQlController(
    private val reservationQueryService: ReservationQueryService,
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
