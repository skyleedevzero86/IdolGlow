package com.sleekydz86.idolglow.productpackage.reservation.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus

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
