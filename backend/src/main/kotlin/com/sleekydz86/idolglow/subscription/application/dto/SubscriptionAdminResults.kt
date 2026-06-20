package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import java.time.DayOfWeek

data class SubscriptionLatestContentResult(
    val contentType: String,
    val contentTypeLabel: String,
    val title: String,
    val slug: String,
    val summary: String?,
    val publishedAt: String?,
)

data class SubscriptionSubscriberResult(
    val id: Long,
    val email: String,
    val subscribedTargets: List<String>,
    val active: Boolean,
    val source: String,
    val consentedAt: String,
    val subscribedAt: String,
) {
    companion object {
        fun from(subscription: EmailSubscription): SubscriptionSubscriberResult =
            SubscriptionSubscriberResult(
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

data class SubscriptionDispatchResult(
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
        fun from(history: SubscriptionDispatchHistory): SubscriptionDispatchResult =
            SubscriptionDispatchResult(
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

data class SubscriptionScheduleResult(
    val id: Long,
    val contentType: String,
    val contentTypeLabel: String,
    val frequencyType: String,
    val frequencyTypeLabel: String,
    val dayOfWeek: String?,
    val dayOfWeekLabel: String?,
    val dispatchTime: String,
    val active: Boolean,
    val nextDispatchAt: String?,
) {
    companion object {
        fun from(schedule: SubscriptionDispatchSchedule): SubscriptionScheduleResult =
            SubscriptionScheduleResult(
                id = schedule.id,
                contentType = schedule.contentType.name,
                contentTypeLabel = schedule.contentType.label,
                frequencyType = schedule.frequencyType.name,
                frequencyTypeLabel = schedule.frequencyType.label,
                dayOfWeek = schedule.dayOfWeek?.name,
                dayOfWeekLabel = schedule.dayOfWeek?.toKoreanLabel(),
                dispatchTime = "%02d:%02d".format(schedule.dispatchHour, schedule.dispatchMinute),
                active = schedule.active,
                nextDispatchAt = schedule.nextDispatchDateTime()?.asSubscriptionDateTime(),
            )
    }
}

data class SubscriptionOverviewResult(
    val totalActive: Long,
    val totalSubscribers: Long,
    val newsletterSubscriberCount: Long,
    val issueSubscriberCount: Long,
    val totalDispatches: Long,
    val subscribers: List<SubscriptionSubscriberResult>,
    val subscriberPage: Int,
    val subscriberSize: Int,
    val subscriberTotalElements: Long,
    val subscriberTotalPages: Int,
    val subscriberHasNext: Boolean,
    val dispatches: List<SubscriptionDispatchResult>,
    val dispatchPage: Int,
    val dispatchSize: Int,
    val dispatchTotalElements: Long,
    val dispatchTotalPages: Int,
    val dispatchHasNext: Boolean,
    val schedules: List<SubscriptionScheduleResult>,
    val latestContents: List<SubscriptionLatestContentResult>,
) {
    companion object {
        fun create(
            totalActive: Long,
            newsletterSubscriberCount: Long,
            issueSubscriberCount: Long,
            totalDispatches: Long,
            subscribers: List<EmailSubscription>,
            subscriberPage: Int,
            subscriberSize: Int,
            subscriberTotalElements: Long,
            subscriberTotalPages: Int,
            subscriberHasNext: Boolean,
            dispatches: List<SubscriptionDispatchHistory>,
            dispatchPage: Int,
            dispatchSize: Int,
            dispatchTotalElements: Long,
            dispatchTotalPages: Int,
            dispatchHasNext: Boolean,
            schedules: List<SubscriptionDispatchSchedule>,
            latestContents: List<SubscriptionLatestContentResult>,
        ): SubscriptionOverviewResult =
            SubscriptionOverviewResult(
                totalActive = totalActive,
                totalSubscribers = subscriberTotalElements,
                newsletterSubscriberCount = newsletterSubscriberCount,
                issueSubscriberCount = issueSubscriberCount,
                totalDispatches = totalDispatches,
                subscribers = subscribers.map(SubscriptionSubscriberResult::from),
                subscriberPage = subscriberPage,
                subscriberSize = subscriberSize,
                subscriberTotalElements = subscriberTotalElements,
                subscriberTotalPages = subscriberTotalPages,
                subscriberHasNext = subscriberHasNext,
                dispatches = dispatches.map(SubscriptionDispatchResult::from),
                dispatchPage = dispatchPage,
                dispatchSize = dispatchSize,
                dispatchTotalElements = dispatchTotalElements,
                dispatchTotalPages = dispatchTotalPages,
                dispatchHasNext = dispatchHasNext,
                schedules = schedules.map(SubscriptionScheduleResult::from),
                latestContents = latestContents,
            )
    }
}

private fun DayOfWeek.toKoreanLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }
