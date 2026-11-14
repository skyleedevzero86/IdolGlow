package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRecommendationMailSubscriptionJpaRepository :
    JpaRepository<ProductRecommendationMailSubscription, Long> {
    fun findByUserId(userId: Long): ProductRecommendationMailSubscription?

    fun findAllByActiveTrueOrderByIdAsc(): List<ProductRecommendationMailSubscription>
}
