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

data class OperationsMenuStatsGraphQlResponse(
    val productsCount: Int,
    val optionsCount: Int,
    val slotsCount: Int,
    val reservationsPendingCount: Int,
    val reservationsBookedCount: Int,
    val reservationsCompletedCount: Int,
    val reservationsCanceledCount: Int,
    val paymentsPendingCount: Int,
    val paymentsSucceededCount: Int,
    val paymentsFailedCount: Int,
    val paymentsCanceledCount: Int,
    val paymentsExpiredCount: Int,
    val paymentsRefundedCount: Int,
    val paymentsPartialCanceledCount: Int,
) {
    companion object {
        fun from(response: OperationsMenuStatsResponse): OperationsMenuStatsGraphQlResponse =
            OperationsMenuStatsGraphQlResponse(
                productsCount = response.productsCount.toInt(),
                optionsCount = response.optionsCount.toInt(),
                slotsCount = response.slotsCount.toInt(),
                reservationsPendingCount = response.reservationsPendingCount.toInt(),
                reservationsBookedCount = response.reservationsBookedCount.toInt(),
                reservationsCompletedCount = response.reservationsCompletedCount.toInt(),
                reservationsCanceledCount = response.reservationsCanceledCount.toInt(),
                paymentsPendingCount = response.paymentsPendingCount.toInt(),
                paymentsSucceededCount = response.paymentsSucceededCount.toInt(),
                paymentsFailedCount = response.paymentsFailedCount.toInt(),
                paymentsCanceledCount = response.paymentsCanceledCount.toInt(),
                paymentsExpiredCount = response.paymentsExpiredCount.toInt(),
                paymentsRefundedCount = response.paymentsRefundedCount.toInt(),
                paymentsPartialCanceledCount = response.paymentsPartialCanceledCount.toInt(),
            )
    }
}
