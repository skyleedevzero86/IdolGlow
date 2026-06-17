package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalDateTime

data class AdminSubscriptionDispatchResponse(
    val id: Long,
    val contentType: String,
    val contentTypeLabel: String,
    val contentSlug: String,
    val contentTitle: String,
    val contentSummary: String?,
    val dispatchChannel: String,
    val dispatchChannelLabel: String,
    val dispatchStatus: String,
    val dispatchStatusLabel: String,
    val recipientCount: Long,
    val dispatchedAt: String,
) {
    companion object {
        fun from(history: SubscriptionDispatchHistory): AdminSubscriptionDispatchResponse =
            AdminSubscriptionDispatchResponse(
                id = history.id,
                contentType = history.contentType.name,
                contentTypeLabel = history.contentType.label,
                contentSlug = history.contentSlug,
                contentTitle = history.contentTitle,
                contentSummary = history.contentSummary,
                dispatchChannel = history.dispatchChannel.name,
                dispatchChannelLabel = history.dispatchChannel.label,
                dispatchStatus = history.dispatchStatus.name,
                dispatchStatusLabel = history.dispatchStatus.label,
                recipientCount = history.recipientCount,
                dispatchedAt = history.dispatchedAt.asSubscriptionDateTime(),
            )
    }
}
