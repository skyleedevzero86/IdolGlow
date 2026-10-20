package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import java.time.LocalDateTime

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

internal fun LocalDateTime.asSubscriptionDateTime(): String = requireNotNull(IdolGlowDateTimes.toDisplay(this))
