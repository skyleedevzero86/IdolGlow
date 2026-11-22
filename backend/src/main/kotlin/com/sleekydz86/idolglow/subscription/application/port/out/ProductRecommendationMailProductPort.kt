package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailItem
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailProducts

interface ProductRecommendationMailProductPort {
    fun findPopular(size: Int): List<ProductRecommendationMailItem>

    fun findPersonalized(
        userId: Long,
        size: Int,
    ): ProductRecommendationMailProducts
}
