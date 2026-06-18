package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
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

private val subscriptionDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

internal fun LocalDateTime.asSubscriptionDateTime(): String = format(subscriptionDateTimeFormatter)
