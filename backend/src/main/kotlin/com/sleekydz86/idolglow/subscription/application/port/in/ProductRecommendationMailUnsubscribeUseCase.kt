package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribePreviewResult
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeResult

interface ProductRecommendationMailUnsubscribeUseCase {
    fun previewUnsubscribe(token: String?): ProductRecommendationMailUnsubscribePreviewResult

    fun unsubscribeByToken(token: String?): ProductRecommendationMailUnsubscribeResult
}
