package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import java.time.DayOfWeek

data class RegisterSubscriptionCommand(
    val email: String,
    val subscribeNewsletters: Boolean,
    val subscribeIssues: Boolean,
    val source: String,
)

data class UpsertSubscriptionDispatchScheduleCommand(
    val contentType: SubscriptionContentType,
    val frequencyType: SubscriptionDispatchFrequency,
    val dayOfWeek: DayOfWeek?,
    val dispatchTime: String,
    val active: Boolean,
)
