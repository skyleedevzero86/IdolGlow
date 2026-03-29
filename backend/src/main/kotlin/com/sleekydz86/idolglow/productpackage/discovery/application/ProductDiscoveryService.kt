package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import com.sleekydz86.idolglow.productpackage.discovery.infrastructure.ProductDiscoveryQueryRepository
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Transactional(readOnly = true)
@Service
class ProductDiscoveryService(
    private val productDiscoveryQueryRepository: ProductDiscoveryQueryRepository,
    private val userSurveyRepository: UserSurveyRepository,
) {

    fun findPopularProducts(size: Int): List<ProductRankingResponse> =
        buildResponses(
            productIds = productDiscoveryQueryRepository.findPopularProductIds(size),
            preferredTags = emptyList(),
            size = size
        )

    fun findRecommendedProducts(userId: Long, size: Int): List<ProductRankingResponse> {
        val survey = userSurveyRepository.findByUserId(userId)
        val behaviorTags = productDiscoveryQueryRepository.findPreferredTags(userId, PREFERRED_TAG_LIMIT)
        val behaviorTagSet = behaviorTags.toSet()

        val conceptTag = survey?.takeUnless { it.concept == ConceptType.ETC }?.concept?.name
        val signalTags = (behaviorTags + listOfNotNull(conceptTag)).distinct()

        if (survey == null && behaviorTags.isEmpty()) {
            return findPopularProducts(size)
        }

        val placeKeywords =
            survey?.places?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct().orEmpty()
        val visitStart = survey?.visitStartDate
        val visitEnd = survey?.visitEndDate
        val now = LocalDateTime.now(ZoneOffset.UTC)

        var candidateIds = productDiscoveryQueryRepository.findPersonalizedCandidateProductIds(
            signalTags = signalTags,
            visitStart = visitStart,
            visitEnd = visitEnd,
            placeKeywords = placeKeywords,
            now = now,
            limit = CANDIDATE_LIMIT,
        )

        if (candidateIds.isEmpty() && signalTags.isNotEmpty()) {
            candidateIds = productDiscoveryQueryRepository.findRecommendedProductIds(signalTags, size * 10)
        }
        if (candidateIds.isEmpty()) {
            return findPopularProducts(size)
        }

        val tripDayCount =
            if (visitStart != null && visitEnd != null) {
                ChronoUnit.DAYS.between(visitStart, visitEnd).toInt() + 1
            } else {
                0
            }

        val availableDaysByProduct =
            if (survey != null && visitStart != null && visitEnd != null && tripDayCount > 0) {
                productDiscoveryQueryRepository.countAvailableTripDaysByProduct(
                    productIds = candidateIds,
                    visitStart = visitStart,
                    visitEnd = visitEnd,
                    now = now,
                )
            } else {
                emptyMap()
            }

        val products = productDiscoveryQueryRepository.findProductsByIds(candidateIds)
        val productById = products.associateBy { it.id }
        val tagNamesByProductId = productDiscoveryQueryRepository.findTagNamesByProductIds(candidateIds)
        val wishCounts = productDiscoveryQueryRepository.findWishCounts(candidateIds)
        val reviewMetrics = productDiscoveryQueryRepository.findReviewMetrics(candidateIds)

        val ranked = candidateIds.mapNotNull { pid ->
            val product = productById[pid] ?: return@mapNotNull null
            val tags = tagNamesByProductId[pid].orEmpty()
            val tagSet = tags.toSet()
            val metric = reviewMetrics[pid]
            val score = PersonalizedRecommendationScorer.score(
                survey = survey,
                behaviorTags = behaviorTagSet,
                conceptTag = conceptTag,
                placeKeywords = placeKeywords,
                productTags = tagSet,
                product = product,
                availableTripDays = availableDaysByProduct[pid] ?: 0,
                tripDayCount = tripDayCount,
                wishCount = wishCounts[pid] ?: 0L,
                averageRating = metric?.averageRating ?: 0.0,
                reviewCount = metric?.reviewCount ?: 0L,
            )
            pid to score
        }.sortedByDescending { it.second }

        val topIds = ranked.map { it.first }.take(size)

        return topIds.mapNotNull { productId ->
            val product = productById[productId] ?: return@mapNotNull null
            val tagNames = tagNamesByProductId[productId].orEmpty()
            val metric = reviewMetrics[productId]
            ProductRankingResponse.from(
                product = product,
                tagNames = tagNames,
                wishCount = wishCounts[productId] ?: 0L,
                averageRating = metric?.averageRating ?: 0.0,
                reviewCount = metric?.reviewCount ?: 0L,
                matchedTags = PersonalizedRecommendationScorer.matchedPreferenceTags(
                    productTags = tagNames,
                    behaviorTags = behaviorTagSet,
                    conceptTag = conceptTag,
                ),
            )
        }
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
        private const val CANDIDATE_LIMIT = 250
    }
}
