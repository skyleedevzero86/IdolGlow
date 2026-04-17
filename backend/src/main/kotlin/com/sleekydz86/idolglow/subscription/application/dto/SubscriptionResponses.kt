package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionDispatchResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionSubscriberResponse
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SubscriptionRegistrationResponse(
    val id: Long,
    val email: String,
    val subscribedTargets: List<String>,
    val subscribedAt: String,
    val active: Boolean,
)

internal fun EmailSubscription.toRegistrationResponse(): SubscriptionRegistrationResponse =
    SubscriptionRegistrationResponse(
        id = id,
        email = email,
        subscribedTargets = subscribedTargets().map { it.label },
        subscribedAt = requireNotNull(subscribedAt).asSubscriptionDateTime(),
        active = active,
    )

internal fun AdminSubscriptionOverviewResponse.Companion.create(
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
): AdminSubscriptionOverviewResponse =
    AdminSubscriptionOverviewResponse(
        totalActive = totalActive,
        totalSubscribers = subscriberTotalElements,
        newsletterSubscriberCount = newsletterSubscriberCount,
        issueSubscriberCount = issueSubscriberCount,
        totalDispatches = totalDispatches,
        subscribers = subscribers.map(AdminSubscriptionSubscriberResponse::from),
        subscriberPage = subscriberPage,
        subscriberSize = subscriberSize,
        subscriberTotalElements = subscriberTotalElements,
        subscriberTotalPages = subscriberTotalPages,
        subscriberHasNext = subscriberHasNext,
        dispatches = dispatches.map(AdminSubscriptionDispatchResponse::from),
        dispatchPage = dispatchPage,
        dispatchSize = dispatchSize,
        dispatchTotalElements = dispatchTotalElements,
        dispatchTotalPages = dispatchTotalPages,
        dispatchHasNext = dispatchHasNext,
    )

private val subscriptionDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

internal fun LocalDateTime.asSubscriptionDateTime(): String = format(subscriptionDateTimeFormatter)
