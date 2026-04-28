package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalDateTime

data class AdminSubscriptionOverviewResponse(
    @Schema(description = "활성 구독 건수")
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
    val schedules: List<AdminSubscriptionScheduleResponse> = emptyList(),
    val latestContents: List<AdminSubscriptionLatestContentResponse> = emptyList(),
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

data class AdminSubscriptionScheduleResponse(
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
        fun from(schedule: SubscriptionDispatchSchedule): AdminSubscriptionScheduleResponse =
            AdminSubscriptionScheduleResponse(
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

data class AdminSubscriptionLatestContentResponse(
    val contentType: String,
    val contentTypeLabel: String,
    val title: String,
    val slug: String,
    val summary: String?,
    val publishedAt: String?,
)

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

private fun SubscriptionDispatchSchedule.nextDispatchDateTime(
    now: LocalDateTime = LocalDateTime.now(),
): LocalDateTime? {
    if (!active) return null

    val baseCandidate = now
        .withHour(dispatchHour)
        .withMinute(dispatchMinute)
        .withSecond(0)
        .withNano(0)

    val candidate = when (frequencyType.name) {
        "DAILY" -> if (baseCandidate.isAfter(now)) baseCandidate else baseCandidate.plusDays(1)
        else -> {
            val target = dayOfWeek ?: return null
            val daysUntil = ((target.value - now.dayOfWeek.value) + 7) % 7
            val initial = baseCandidate.plusDays(daysUntil.toLong())
            if (daysUntil == 0 && !initial.isAfter(now)) initial.plusWeeks(1) else initial
        }
    }

    return candidate
}
