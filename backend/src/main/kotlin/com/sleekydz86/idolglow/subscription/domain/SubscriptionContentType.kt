package com.sleekydz86.idolglow.subscription.domain

enum class SubscriptionContentType(
    val label: String,
    val audience: SubscriptionAudience,
) {
    NEWSLETTER("뉴스레터", SubscriptionAudience.NEWSLETTER),
    IDOLGLOW_ISSUE("웹진", SubscriptionAudience.IDOLGLOW_ISSUE),
    PRODUCT_RECOMMENDATION("상품 추천", SubscriptionAudience.PRODUCT_RECOMMENDATION),
}
