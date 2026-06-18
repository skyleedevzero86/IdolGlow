package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionOverviewResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionScheduleResult
import com.sleekydz86.idolglow.subscription.application.dto.UpsertSubscriptionDispatchScheduleCommand

interface SubscriptionAdminUseCase {
    fun findOverview(
        subscriberPage: Int,
        subscriberSize: Int,
        dispatchPage: Int,
        dispatchSize: Int,
    ): SubscriptionOverviewResult

    fun upsertDispatchSchedule(command: UpsertSubscriptionDispatchScheduleCommand): SubscriptionScheduleResult
}
