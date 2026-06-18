package com.sleekydz86.idolglow.productpackage.admin.graphql

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse

data class AdminSubscriptionOverviewGraphQlResponse(
    val totalActive: Int,
) {
    companion object {
        fun from(response: AdminSubscriptionOverviewResponse): AdminSubscriptionOverviewGraphQlResponse =
            AdminSubscriptionOverviewGraphQlResponse(
                totalActive = response.totalActive.toInt(),
            )
    }
}
