package com.sleekydz86.idolglow.productpackage.admin.adapter.graphql

import com.sleekydz86.idolglow.admin.adapter.web.dto.AdminSubscriptionOverviewResponse

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
