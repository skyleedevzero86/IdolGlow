package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse

interface SubscriptionAdminUseCase {
    fun findOverview(
        subscriberPage: Int,
        subscriberSize: Int,
        dispatchPage: Int,
        dispatchSize: Int,
    ): AdminSubscriptionOverviewResponse
}
