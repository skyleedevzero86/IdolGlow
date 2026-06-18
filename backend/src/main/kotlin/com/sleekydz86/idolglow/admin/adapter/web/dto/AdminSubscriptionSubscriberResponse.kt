package com.sleekydz86.idolglow.admin.adapter.web.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription

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
