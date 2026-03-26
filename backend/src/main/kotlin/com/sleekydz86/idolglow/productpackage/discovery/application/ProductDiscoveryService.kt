package com.sleekydz86.idolglow.productpackage.discovery.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.sortedWith
import kotlin.comparisons.thenByDescending

@Transactional(readOnly = true)
@Service
class ProductDiscoveryService(
    private val productDiscoveryQueryRepository: ProductDiscoveryQueryRepository,
) {

    fun findPopularProducts(size: Int): List<ProductRankingResponse> =
        buildResponses(
            productIds = productDiscoveryQueryRepository.findPopularProductIds(size),
            preferredTags = emptyList(),
            size = size
        )

    fun findRecommendedProducts(userId: Long, size: Int): List<ProductRankingResponse> {
        val preferredTags = productDiscoveryQueryRepository.findPreferredTags(userId, PREFERRED_TAG_LIMIT)
        if (preferredTags.isEmpty()) {
            return findPopularProducts(size)
        }

        val candidateIds = productDiscoveryQueryRepository.findRecommendedProductIds(preferredTags, size)
        if (candidateIds.isEmpty()) {
            return findPopularProducts(size)
        }

        return buildResponses(candidateIds, preferredTags, size)
            .sortedWith(
                compareByDescending<ProductRankingResponse> { it.matchedTags.size }
                    .thenByDescending { it.wishCount }
                    .thenByDescending { it.averageRating }
                    .thenByDescending { it.reviewCount }
                    .thenByDescending { it.id }
            )
            .take(size)
    }

    private fun buildResponses(
        productIds: List<Long>,
        preferredTags: List<String>,
        size: Int,
    ): List<ProductRankingResponse> {
        if (productIds.isEmpty()) {
            return emptyList()
        }

        val products = productDiscoveryQueryRepository.findProductsByIds(productIds)
        val tagNamesByProductId = productDiscoveryQueryRepository.findTagNamesByProductIds(productIds)
        val wishCounts = productDiscoveryQueryRepository.findWishCounts(productIds)
        val reviewMetrics = productDiscoveryQueryRepository.findReviewMetrics(productIds)
        val productById = products.associateBy { it.id }

        return productIds.mapNotNull { productId ->
            val product = productById[productId] ?: return@mapNotNull null
            val tagNames = tagNamesByProductId[productId].orEmpty()
            val metric = reviewMetrics[productId]
            ProductRankingResponse.from(
                product = product,
                tagNames = tagNames,
                wishCount = wishCounts[productId] ?: 0L,
                averageRating = metric?.averageRating ?: 0.0,
                reviewCount = metric?.reviewCount ?: 0L,
                matchedTags = tagNames.filter { it in preferredTags }
            )
        }.take(size)
    }

    companion object {
        private const val PREFERRED_TAG_LIMIT = 5
    }
}
