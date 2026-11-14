package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription

interface ProductRecommendationMailSubscriptionPort {
    fun findById(id: Long): ProductRecommendationMailSubscription?

    fun findByUserId(userId: Long): ProductRecommendationMailSubscription?

    fun findAllActive(): List<ProductRecommendationMailSubscription>

    fun save(subscription: ProductRecommendationMailSubscription): ProductRecommendationMailSubscription
}
