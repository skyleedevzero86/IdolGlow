package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import io.swagger.v3.oas.annotations.media.Schema

data class AdminSubscriptionOverviewResponse(
    @Schema(description = "Number of active subscriptions")
    val totalActive: Long = 0,
    val totalSubscribers: Long = 0,
    val newsletterSubscriberCount: Long = 0,
    val issueSubscriberCount: Long = 0,
    val totalDispatches: Long = 0,
    val subscribers: List<AdminSubscriptionSubscriberResponse> = emptyList(),
    val subscriberPage: Int = 1,
    val subscriberSize: Int = 10,
    val subscriberTotalElements: Long = 0,
    val subscriberTotalPages: Int = 0,
    val subscriberHasNext: Boolean = false,
    val dispatches: List<AdminSubscriptionDispatchResponse> = emptyList(),
    val dispatchPage: Int = 1,
    val dispatchSize: Int = 10,
    val dispatchTotalElements: Long = 0,
    val dispatchTotalPages: Int = 0,
    val dispatchHasNext: Boolean = false,
) {
    companion object
}

data class AdminSubscriptionSubscriberResponse(
    val id: Long,
    val email: String,
    val subscribedTargets: List<String>,
    val active: Boolean,
    val source: String,
    val consentedAt: String,
    val subscribedAt: String,
) {
    companion object {
        fun from(subscription: EmailSubscription): AdminSubscriptionSubscriberResponse =
            AdminSubscriptionSubscriberResponse(
                id = subscription.id,
                email = subscription.email,
                subscribedTargets = subscription.subscribedTargets().map { it.label },
                active = subscription.active,
                source = subscription.subscriptionSource,
                consentedAt = subscription.consentedAt.asSubscriptionDateTime(),
                subscribedAt = subscription.subscribedAt.asSubscriptionDateTime(),
            )
    }
}

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
