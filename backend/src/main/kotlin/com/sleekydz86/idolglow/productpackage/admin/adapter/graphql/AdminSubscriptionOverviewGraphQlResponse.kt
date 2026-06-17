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
