package com.sleekydz86.idolglow.subscription.domain

enum class SubscriptionAudience(
    val label: String,
) {
    NEWSLETTER("소식지"),
    IDOLGLOW_ISSUE("호별보기"),
    PRODUCT_RECOMMENDATION("상품 추천"),
}
