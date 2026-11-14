package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import java.time.LocalDateTime

interface ProductRecommendationMailSubscriptionPort {
    fun findById(id: Long): ProductRecommendationMailSubscription?

    fun findByUserId(userId: Long): ProductRecommendationMailSubscription?

    fun findAllActive(): List<ProductRecommendationMailSubscription>

    fun findAllConsentReminderDue(lastNoticeAtOrBefore: LocalDateTime): List<ProductRecommendationMailSubscription>

    fun save(subscription: ProductRecommendationMailSubscription): ProductRecommendationMailSubscription
}
