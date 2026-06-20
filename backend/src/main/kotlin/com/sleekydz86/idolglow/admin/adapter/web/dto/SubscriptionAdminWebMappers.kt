package com.sleekydz86.idolglow.admin.adapter.web.dto

import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionDispatchResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionLatestContentResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionOverviewResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionScheduleResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionSubscriberResult

fun SubscriptionOverviewResult.toWebResponse(): AdminSubscriptionOverviewResponse =
    AdminSubscriptionOverviewResponse(
        totalActive = totalActive,
        totalSubscribers = totalSubscribers,
        newsletterSubscriberCount = newsletterSubscriberCount,
        issueSubscriberCount = issueSubscriberCount,
        totalDispatches = totalDispatches,
        subscribers = subscribers.map { it.toWebResponse() },
        subscriberPage = subscriberPage,
        subscriberSize = subscriberSize,
        subscriberTotalElements = subscriberTotalElements,
        subscriberTotalPages = subscriberTotalPages,
        subscriberHasNext = subscriberHasNext,
        dispatches = dispatches.map { it.toWebResponse() },
        dispatchPage = dispatchPage,
        dispatchSize = dispatchSize,
        dispatchTotalElements = dispatchTotalElements,
        dispatchTotalPages = dispatchTotalPages,
        dispatchHasNext = dispatchHasNext,
        schedules = schedules.map { it.toWebResponse() },
        latestContents = latestContents.map { it.toWebResponse() },
    )

fun SubscriptionSubscriberResult.toWebResponse(): AdminSubscriptionSubscriberResponse =
    AdminSubscriptionSubscriberResponse(
        id = id,
        email = email,
        subscribedTargets = subscribedTargets,
        active = active,
        source = source,
        consentedAt = consentedAt,
        subscribedAt = subscribedAt,
    )

fun SubscriptionDispatchResult.toWebResponse(): AdminSubscriptionDispatchResponse =
    AdminSubscriptionDispatchResponse(
        id = id,
        contentType = contentType,
        contentTypeLabel = contentTypeLabel,
        contentSlug = contentSlug,
        contentTitle = contentTitle,
        contentSummary = contentSummary,
        dispatchChannel = dispatchChannel,
        dispatchChannelLabel = dispatchChannelLabel,
        dispatchStatus = dispatchStatus,
        dispatchStatusLabel = dispatchStatusLabel,
        recipientCount = recipientCount,
        dispatchedAt = dispatchedAt,
    )

fun SubscriptionScheduleResult.toWebResponse(): AdminSubscriptionScheduleResponse =
    AdminSubscriptionScheduleResponse(
        id = id,
        contentType = contentType,
        contentTypeLabel = contentTypeLabel,
        frequencyType = frequencyType,
        frequencyTypeLabel = frequencyTypeLabel,
        dayOfWeek = dayOfWeek,
        dayOfWeekLabel = dayOfWeekLabel,
        dispatchTime = dispatchTime,
        active = active,
        nextDispatchAt = nextDispatchAt,
    )

fun SubscriptionLatestContentResult.toWebResponse(): AdminSubscriptionLatestContentResponse =
    AdminSubscriptionLatestContentResponse(
        contentType = contentType,
        contentTypeLabel = contentTypeLabel,
        title = title,
        slug = slug,
        summary = summary,
        publishedAt = publishedAt,
    )
