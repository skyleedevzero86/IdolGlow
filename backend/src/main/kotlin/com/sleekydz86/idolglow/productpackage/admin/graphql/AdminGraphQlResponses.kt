package com.sleekydz86.idolglow.productpackage.admin.graphql

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ReservationDashboardResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus

data class AdminSubscriptionOverviewGraphQlResponse(
    val totalActive: Int,
) {
    companion object {
        fun from(response: AdminSubscriptionOverviewResponse): AdminSubscriptionOverviewGraphQlResponse =
            AdminSubscriptionOverviewGraphQlResponse(
                totalActive = response.totalActive.toInt()
            )
    }
}

data class AdminReservationDashboardGraphQlResponse(
    val pendingCount: Int,
    val bookedCount: Int,
    val completedCount: Int,
    val canceledCount: Int,
    val paymentPendingCount: Int,
    val paymentSucceededCount: Int,
    val paymentFailedCount: Int,
    val paymentCanceledCount: Int,
    val paymentExpiredCount: Int,
    val recentReservations: List<AdminReservationSummaryGraphQlResponse>,
) {
    companion object {
        fun from(response: ReservationDashboardResponse): AdminReservationDashboardGraphQlResponse =
            AdminReservationDashboardGraphQlResponse(
                pendingCount = response.pendingCount.toInt(),
                bookedCount = response.bookedCount.toInt(),
                completedCount = response.completedCount.toInt(),
                canceledCount = response.canceledCount.toInt(),
                paymentPendingCount = response.paymentPendingCount.toInt(),
                paymentSucceededCount = response.paymentSucceededCount.toInt(),
                paymentFailedCount = response.paymentFailedCount.toInt(),
                paymentCanceledCount = response.paymentCanceledCount.toInt(),
                paymentExpiredCount = response.paymentExpiredCount.toInt(),
                recentReservations = response.recentReservations.map(AdminReservationSummaryGraphQlResponse::from)
            )
    }
}

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
                paymentFailureReason = response.paymentFailureReason
            )
    }
}

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
                holdExpiresAt = response.holdExpiresAt.asGraphQlValue()
            )
    }
}
