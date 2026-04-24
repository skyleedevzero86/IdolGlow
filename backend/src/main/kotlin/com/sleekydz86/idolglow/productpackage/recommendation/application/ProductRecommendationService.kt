package com.sleekydz86.idolglow.productpackage.recommendation.application

import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.recommendation.application.dto.ProductAdminRecommendationUpdateResponse
import com.sleekydz86.idolglow.productpackage.recommendation.application.dto.ProductRecommendationScoreUpdateResponse
import com.sleekydz86.idolglow.productpackage.recommendation.domain.ProductLatestKoreaRecommendation
import com.sleekydz86.idolglow.productpackage.recommendation.infrastructure.ProductLatestKoreaRecommendationJpaRepository
import com.sleekydz86.idolglow.productpackage.recommendation.infrastructure.RecommendationProductJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ProductRecommendationService(
    private val latestKoreaRepository: ProductLatestKoreaRecommendationJpaRepository,
    private val productRepository: RecommendationProductJpaRepository,
    private val aggregateImageQueryService: AggregateImageQueryService,
) {
    @Transactional
    fun replaceLatestInKorea(productIds: List<Long>): Int {
        val normalized = productIds.distinct()
        require(normalized.isNotEmpty()) { "추천 상품 ID를 1개 이상 전달해 주세요." }
        require(normalized.all { productRepository.existsById(it) }) { "존재하지 않는 상품 ID가 포함되어 있습니다." }

        latestKoreaRepository.deleteAllInBatch()
        val rows = normalized.mapIndexed { index, productId ->
            ProductLatestKoreaRecommendation(
                displayOrder = index + 1,
                productId = productId,
            )
        }
        latestKoreaRepository.saveAll(rows)
        return rows.size
    }

    fun findLatestInKorea(size: Int, tagName: String?): List<ProductPagingQueryResponse> {
        val resolvedSize = size.coerceIn(1, 50)
        val normalizedTag = tagName?.trim()?.takeIf { it.isNotEmpty() }
        val orderedIds = latestKoreaRepository.findAllByOrderByDisplayOrderAscIdAsc()
            .map { it.productId }
        if (orderedIds.isEmpty()) {
            return emptyList()
        }

        val productsById = productRepository.findAllById(orderedIds).associateBy { it.id }
        val filtered = orderedIds.asSequence()
            .mapNotNull { productsById[it] }
            .filter { product ->
                normalizedTag == null || product.productTags.any { tag ->
                    tag.tagName.equals(normalizedTag, ignoreCase = true)
                }
            }
            .take(resolvedSize)
            .toList()
        return toPagingResponses(filtered)
    }

    @Transactional
    fun updateAdminRecommendation(productId: Long, recommended: Boolean): ProductAdminRecommendationUpdateResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId") }
        product.isRecommended = recommended
        productRepository.save(product)
        return ProductAdminRecommendationUpdateResponse(
            productId = product.id,
            recommended = product.isRecommended,
        )
    }

    @Transactional
    fun updateRecommendationScore(productId: Long, score: Int): ProductRecommendationScoreUpdateResponse {
        require(score >= 0) { "추천 점수는 0 이상이어야 합니다." }
        val product = productRepository.findById(productId)
            .orElseThrow { IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId") }
        product.recommendationScore = score
        productRepository.save(product)
        return ProductRecommendationScoreUpdateResponse(
            productId = product.id,
            recommendationScore = product.recommendationScore,
        )
    }

    fun findAdminRecommended(size: Int, tagName: String?, searchKeyword: String?): List<ProductPagingQueryResponse> {
        val resolvedSize = size.coerceIn(1, 50)
        val normalizedTag = tagName?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedKeyword = searchKeyword?.trim()?.takeIf { it.isNotEmpty() }
        val products = productRepository.findAdminPicked(
            tag = normalizedTag,
            keyword = normalizedKeyword,
            pageable = PageRequest.of(0, resolvedSize),
        )
        return toPagingResponses(products)
    }

    private fun toPagingResponses(products: List<com.sleekydz86.idolglow.productpackage.product.domain.Product>): List<ProductPagingQueryResponse> {
        if (products.isEmpty()) return emptyList()
        val thumbnailById = aggregateImageQueryService.firstProductImageUrlByProductIds(products.map { it.id })
        return products.map { product ->
            ProductPagingQueryResponse.from(
                product = product,
                thumbnailUrl = thumbnailById[product.id],
                tourAttractionPickCount = runCatching {
                    if (product.tourAttractionPicksJson.isNullOrBlank()) 0 else 1
                }.getOrDefault(0),
            )
        }
    }
}
