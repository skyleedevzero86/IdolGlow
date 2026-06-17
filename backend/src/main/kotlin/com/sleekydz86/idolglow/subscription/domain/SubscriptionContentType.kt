package com.sleekydz86.idolglow.subscription.domain

enum class SubscriptionContentType(val label: String, val audience: SubscriptionAudience) {
    NEWSLETTER("뉴스레터", SubscriptionAudience.NEWSLETTER),
    WEBZINE_ISSUE("웹진", SubscriptionAudience.WEBZINE_ISSUE),
}
