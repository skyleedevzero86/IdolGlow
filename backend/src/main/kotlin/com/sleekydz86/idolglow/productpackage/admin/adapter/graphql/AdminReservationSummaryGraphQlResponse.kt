package com.sleekydz86.idolglow.productpackage.admin.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus

data class AdminReservationSummaryGraphQlResponse(
    val reservationId: String,
    val userId: String,
    val productId: String,
    val productName: String,
    val status: ReservationStatus,
    val totalPrice: String,
    val visitDate: String,
    val visitStartTime: String,
    val visitEndTime: String,
    val expiresAt: String?,
    val confirmedAt: String?,
    val canceledAt: String?,
    val cancelReason: ReservationCancelReason?,
    val paymentReference: String?,
    val paymentStatus: String?,
    val paymentFailureReason: String?,
) {
    companion object {
        fun from(response: AdminReservationSummaryResponse): AdminReservationSummaryGraphQlResponse =
            AdminReservationSummaryGraphQlResponse(
                reservationId = response.reservationId.asGraphQlId(),
                userId = response.userId.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                productName = response.productName,
                status = response.status,
                totalPrice = response.totalPrice.asGraphQlNumber(),
                visitDate = requireNotNull(response.visitDate.asGraphQlValue()),
                visitStartTime = requireNotNull(response.visitStartTime.asGraphQlValue()),
                visitEndTime = requireNotNull(response.visitEndTime.asGraphQlValue()),
                expiresAt = response.expiresAt.asGraphQlValue(),
                confirmedAt = response.confirmedAt.asGraphQlValue(),
                canceledAt = response.canceledAt.asGraphQlValue(),
                cancelReason = response.cancelReason,
                paymentReference = response.paymentReference,
                paymentStatus = response.paymentStatus?.name,
                paymentFailureReason = response.paymentFailureReason,
            )
    }
}
