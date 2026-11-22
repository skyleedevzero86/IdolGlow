package com.sleekydz86.idolglow.admin.adapter.web.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientCounts

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
    val sentCount: Long,
    val failedCount: Long,
    val skippedCount: Long,
    val dispatchedAt: String,
) {
    companion object {
        fun from(
            history: SubscriptionDispatchHistory,
            counts: SubscriptionDispatchRecipientCounts = SubscriptionDispatchRecipientCounts.EMPTY,
        ): AdminSubscriptionDispatchResponse =
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
                sentCount = counts.sentCount,
                failedCount = counts.failedCount,
                skippedCount = counts.skippedCount,
                dispatchedAt = history.dispatchedAt.asSubscriptionDateTime(),
            )
    }
}
