package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ProductRecommendationMailSubscriptionRepositoryImpl(
    private val productRecommendationMailSubscriptionJpaRepository: ProductRecommendationMailSubscriptionJpaRepository,
) : ProductRecommendationMailSubscriptionPort {
    override fun findById(id: Long): ProductRecommendationMailSubscription? =
        productRecommendationMailSubscriptionJpaRepository.findByIdOrNull(id)

    override fun findByUserId(userId: Long): ProductRecommendationMailSubscription? =
        productRecommendationMailSubscriptionJpaRepository.findByUserId(userId)

    override fun save(subscription: ProductRecommendationMailSubscription): ProductRecommendationMailSubscription =
        productRecommendationMailSubscriptionJpaRepository.save(subscription)
}
