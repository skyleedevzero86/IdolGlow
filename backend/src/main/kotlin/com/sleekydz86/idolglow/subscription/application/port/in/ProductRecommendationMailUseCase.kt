package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSettingResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscribeProductRecommendationMailCommand

interface ProductRecommendationMailUseCase {
    fun findMine(userId: Long): ProductRecommendationMailSettingResult

    fun subscribe(command: SubscribeProductRecommendationMailCommand): ProductRecommendationMailSettingResult

    fun unsubscribeMine(userId: Long): ProductRecommendationMailSettingResult
}
