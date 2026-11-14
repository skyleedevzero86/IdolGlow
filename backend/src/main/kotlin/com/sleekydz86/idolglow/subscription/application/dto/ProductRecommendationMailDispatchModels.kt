package com.sleekydz86.idolglow.subscription.application.dto

import java.math.BigDecimal

enum class ProductRecommendationMailBasis {
    PERSONALIZED,
    POPULAR,
}

data class ProductRecommendationMailItem(
    val productId: Long,
    val name: String,
    val minPrice: BigDecimal,
    val reasons: List<String> = emptyList(),
    val highlights: List<String> = emptyList(),
)

data class ProductRecommendationMailProducts(
    val items: List<ProductRecommendationMailItem>,
    val fellBackToPopular: Boolean,
)

data class ProductRecommendationMailPlan(
    val subscriptionId: Long,
    val email: String,
    val basis: ProductRecommendationMailBasis,
    val surveyFallback: Boolean,
    val items: List<ProductRecommendationMailItem>,
    val unsubscribeToken: String,
)
