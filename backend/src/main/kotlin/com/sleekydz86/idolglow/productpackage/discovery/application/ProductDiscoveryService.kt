package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import com.sleekydz86.idolglow.productpackage.discovery.infrastructure.ProductDiscoveryQueryRepository
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Transactional(readOnly = true)
@Service
class ProductDiscoveryService(
    private val productDiscoveryQueryRepository: ProductDiscoveryQueryRepository,
    private val userSurveyRepository: UserSurveyRepository,
    private val aggregateImageQueryService: AggregateImageQueryService,
) {
    fun findPopularProducts(size: Int): List<ProductRankingResponse> =
        buildResponses(
            productIds = productDiscoveryQueryRepository.findPopularProductIds(size),
            preferredTags = emptyList(),
            size = size,
        )

    fun findRecommendedProducts(
        userId: Long,
        size: Int,
    ): List<ProductRankingResponse> = findRecommendedProducts(signalsOf(userId), size)

    fun signalsOf(userId: Long): RecommendationSignals =
        RecommendationSignals.fromUserSurvey(
            survey = userSurveyRepository.findByUserId(userId),
            behaviorTags = productDiscoveryQueryRepository.findPreferredTags(userId, PREFERRED_TAG_LIMIT).toSet(),
        )

    fun findRecommendedProducts(
        signals: RecommendationSignals,
        size: Int,
    ): List<ProductRankingResponse> {
        val candidateIds = if (signals.isEmpty()) emptyList() else findCandidateIds(signals, size)
        if (candidateIds.isEmpty()) {
            return findPopularProducts(size).map { it.copy(fallback = RecommendationFallback.POPULAR) }
        }
        return rank(signals, candidateIds, size)
    }

    private fun findCandidateIds(
        signals: RecommendationSignals,
        size: Int,
    ): List<Long> {
        val signalTags = signals.candidateTags()
        val candidateIds =
            productDiscoveryQueryRepository.findPersonalizedCandidateProductIds(
                signalTags = signalTags,
                visitStart = signals.visitStart,
                visitEnd = signals.visitEnd,
                placeKeywords = signals.placeKeywords,
                now = LocalDateTime.now(ZoneOffset.UTC),
                limit = CANDIDATE_LIMIT,
            )
        if (candidateIds.isNotEmpty() || signalTags.isEmpty()) {
            return candidateIds
        }
        return productDiscoveryQueryRepository.findRecommendedProductIds(signalTags, size * FALLBACK_TAG_MULTIPLIER)
    }

    private fun rank(
        signals: RecommendationSignals,
        candidateIds: List<Long>,
        size: Int,
    ): List<ProductRankingResponse> {
        val availableDaysByProduct = availableTripDays(signals, candidateIds)
        val productById = productDiscoveryQueryRepository.findProductsByIds(candidateIds).associateBy { it.id }
        val tagNamesByProductId = productDiscoveryQueryRepository.findTagNamesByProductIds(candidateIds)
        val wishCounts = productDiscoveryQueryRepository.findWishCounts(candidateIds)
        val reviewMetrics = productDiscoveryQueryRepository.findReviewMetrics(candidateIds)

        val ranked =
            candidateIds
                .mapNotNull { productId ->
                    val product = productById[productId] ?: return@mapNotNull null
                    val tagNames = tagNamesByProductId[productId].orEmpty()
                    val metric = reviewMetrics[productId]
                    val wishCount = wishCounts[productId] ?: 0L
                    val averageRating = metric?.averageRating ?: 0.0
                    val reviewCount = metric?.reviewCount ?: 0L
                    val scored =
                        PersonalizedRecommendationScorer.score(
                            signals = signals,
                            productTags = tagNames.toSet(),
                            product = product,
                            metrics =
                                RecommendationProductMetrics(
                                    availableTripDays = availableDaysByProduct[productId] ?: 0,
                                    wishCount = wishCount,
                                    averageRating = averageRating,
                                    reviewCount = reviewCount,
                                ),
                        )
                    scored.score to
                        ProductRankingResponse.from(
                            product = product,
                            tagNames = tagNames,
                            wishCount = wishCount,
                            averageRating = averageRating,
                            reviewCount = reviewCount,
                            matchedTags = PersonalizedRecommendationScorer.matchedPreferenceTags(tagNames, signals),
                            reasons = scored.reasons,
                        )
                }.sortedByDescending { it.first }

        return withThumbnails(ranked.take(size).map { it.second })
    }

    private fun availableTripDays(
        signals: RecommendationSignals,
        candidateIds: List<Long>,
    ): Map<Long, Int> {
        val visitStart = signals.visitStart
        val visitEnd = signals.visitEnd
        if (visitStart == null || visitEnd == null || signals.tripDayCount() <= 0) {
            return emptyMap()
        }
        return productDiscoveryQueryRepository.countAvailableTripDaysByProduct(
            productIds = candidateIds,
            visitStart = visitStart,
            visitEnd = visitEnd,
            now = LocalDateTime.now(ZoneOffset.UTC),
        )
    }

    private fun withThumbnails(responses: List<ProductRankingResponse>): List<ProductRankingResponse> {
        if (responses.isEmpty()) return responses
        val thumbs = aggregateImageQueryService.firstProductImageUrlByProductIds(responses.map { it.id })
        return responses.map { it.copy(thumbnailUrl = thumbs[it.id]) }
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

        return withThumbnails(
            productIds
                .mapNotNull { productId ->
                    val product = productById[productId] ?: return@mapNotNull null
                    val tagNames = tagNamesByProductId[productId].orEmpty()
                    val metric = reviewMetrics[productId]
                    ProductRankingResponse.from(
                        product = product,
                        tagNames = tagNames,
                        wishCount = wishCounts[productId] ?: 0L,
                        averageRating = metric?.averageRating ?: 0.0,
                        reviewCount = metric?.reviewCount ?: 0L,
                        matchedTags = tagNames.filter { it in preferredTags },
                    )
                }.take(size),
        )
    }

    companion object {
        private const val PREFERRED_TAG_LIMIT = 5
        private const val CANDIDATE_LIMIT = 250
        private const val FALLBACK_TAG_MULTIPLIER = 10
    }
}
