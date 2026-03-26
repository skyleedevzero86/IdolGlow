package com.sleekydz86.idolglow.productpackage.reservation.graphql

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
            ?: throw IllegalArgumentException("id must be numeric.")

        return ReservationSummaryGraphQlResponse.from(
            reservationQueryService.findReservationDetail(
                reservationId = reservationId,
                userId = authenticatedUserIdResolver.resolveRequired()
            )
        )
    }
}

data class ReservationSummaryGraphQlResponse(
    val reservationId: String,
    val status: ReservationStatus,
    val productId: String,
    val productName: String,
    val productDescription: String,
    val totalPrice: String,
    val visitDate: String,
    val visitStartTime: String,
    val visitEndTime: String,
    val attractions: List<String>,
    val expiresAt: String?,
    val confirmedAt: String?,
    val canceledAt: String?,
    val cancelReason: ReservationCancelReason?,
) {
    companion object {
        fun from(response: ReservationSummaryResponse): ReservationSummaryGraphQlResponse =
            ReservationSummaryGraphQlResponse(
                reservationId = response.reservationId.asGraphQlId(),
                status = response.status,
                productId = response.productId.asGraphQlId(),
                productName = response.productName,
                productDescription = response.productDescription,
                totalPrice = response.totalPrice.asGraphQlNumber(),
                visitDate = requireNotNull(response.visitDate.asGraphQlValue()),
                visitStartTime = requireNotNull(response.visitStartTime.asGraphQlValue()),
                visitEndTime = requireNotNull(response.visitEndTime.asGraphQlValue()),
                attractions = response.attractions,
                expiresAt = response.expiresAt.asGraphQlValue(),
                confirmedAt = response.confirmedAt.asGraphQlValue(),
                canceledAt = response.canceledAt.asGraphQlValue(),
                cancelReason = response.cancelReason
            )
    }
}
