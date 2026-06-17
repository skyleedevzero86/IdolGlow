package com.sleekydz86.idolglow.productpackage.admin.graphql

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSlotResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.OperationsMenuStatsResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ReservationDashboardResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus

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
