package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalDateTime

data class AdminSubscriptionLatestContentResponse(
    val contentType: String,
    val contentTypeLabel: String,
    val title: String,
    val slug: String,
    val summary: String?,
    val publishedAt: String?,
)
