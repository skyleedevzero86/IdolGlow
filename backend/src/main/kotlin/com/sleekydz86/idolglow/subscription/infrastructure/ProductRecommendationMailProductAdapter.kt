package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationFallback
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationReasonCode
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailItem
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailProducts
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailProductPort
import org.springframework.stereotype.Component

@Component
class ProductRecommendationMailProductAdapter(
    private val productDiscoveryService: ProductDiscoveryService,
) : ProductRecommendationMailProductPort {
    override fun findPopular(size: Int): List<ProductRecommendationMailItem> =
        productDiscoveryService.findPopularProducts(size).map(::toItem)

    override fun findPersonalized(
        userId: Long,
        size: Int,
    ): ProductRecommendationMailProducts {
        val products =
            productDiscoveryService.findRecommendedProducts(
                productDiscoveryService.signalsOf(userId),
                size,
            )
        return ProductRecommendationMailProducts(
            items = products.map(::toItem),
            fellBackToPopular = products.any { it.fallback == RecommendationFallback.POPULAR },
        )
    }

    private fun toItem(product: ProductRankingResponse): ProductRecommendationMailItem =
        ProductRecommendationMailItem(
            productId = product.id,
            name = product.name,
            minPrice = product.minPrice,
            reasons = product.reasons.map { it.label },
            highlights = product.reasons.filter { it.code in HIGHLIGHT_CODES }.map { it.label },
        )

    companion object {
        private val HIGHLIGHT_CODES =
            setOf(RecommendationReasonCode.CONCEPT_MATCH, RecommendationReasonCode.PLACE_MATCH)
    }
}
