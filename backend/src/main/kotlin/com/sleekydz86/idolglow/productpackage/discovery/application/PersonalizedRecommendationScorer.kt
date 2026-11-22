package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import kotlin.math.ln

object PersonalizedRecommendationScorer {
    private const val WEIGHT_CONCEPT_TAG = 4.0
    private const val WEIGHT_BEHAVIOR_TAG = 2.5
    private const val WEIGHT_EXTRA_TAG = WEIGHT_BEHAVIOR_TAG
    private const val WEIGHT_PLACE = 2.0
    private const val WEIGHT_IDOL_TEXT = 1.2
    private const val WEIGHT_SCHEDULE_FIT = 5.0
    private const val WEIGHT_WISH_LOG = 0.35
    private const val WEIGHT_RATING = 0.45
    private const val WEIGHT_REVIEW_LOG = 0.15
    private const val MAX_RATING = 5.0

    fun score(
        signals: RecommendationSignals,
        productTags: Set<String>,
        product: Product,
        metrics: RecommendationProductMetrics,
    ): RecommendationScore {
        val blob = productSearchBlob(product)
        val parts =
            listOfNotNull(
                conceptPart(signals, productTags),
                tagPart(productTags, signals.behaviorTags, WEIGHT_BEHAVIOR_TAG, RecommendationReasonCode.BEHAVIOR_TAG),
                tagPart(productTags, signals.extraTags, WEIGHT_EXTRA_TAG, RecommendationReasonCode.EXTRA_TAG),
                textPart(blob, signals.placeKeywords, WEIGHT_PLACE, RecommendationReasonCode.PLACE_MATCH),
                textPart(blob, signals.idolTokens, WEIGHT_IDOL_TEXT, RecommendationReasonCode.IDOL_MATCH),
                schedulePart(signals.tripDayCount(), metrics.availableTripDays),
            )
        val popularity =
            WEIGHT_WISH_LOG * ln((metrics.wishCount + 1).toDouble()) +
                WEIGHT_RATING * metrics.averageRating.coerceIn(0.0, MAX_RATING) +
                WEIGHT_REVIEW_LOG * ln((metrics.reviewCount + 1).toDouble())
        return RecommendationScore(
            score = parts.sumOf { it.first } + popularity,
            reasons = parts.map { it.second },
        )
    }

    fun matchedPreferenceTags(
        productTags: List<String>,
        signals: RecommendationSignals,
    ): List<String> {
        val concepts = signals.concepts()
        return productTags
            .filter { tag ->
                tag in signals.behaviorTags ||
                    tag in signals.extraTags ||
                    concepts.any { ConceptAliases.matches(it, tag) }
            }.distinct()
    }

    private fun conceptPart(
        signals: RecommendationSignals,
        productTags: Set<String>,
    ): Pair<Double, RecommendationReason>? {
        val matched = signals.concepts().filter { concept -> productTags.any { ConceptAliases.matches(concept, it) } }
        if (matched.isEmpty()) return null
        val labels = matched.map { ConceptAliases.labelOf(it) ?: it.name }
        return WEIGHT_CONCEPT_TAG * matched.size to
            RecommendationReason(RecommendationReasonCode.CONCEPT_MATCH, "선호 컨셉 ${quoted(labels)} 태그")
    }

    private fun tagPart(
        productTags: Set<String>,
        signalTags: Set<String>,
        weight: Double,
        code: RecommendationReasonCode,
    ): Pair<Double, RecommendationReason>? {
        val matched = productTags.filter { it in signalTags }
        if (matched.isEmpty()) return null
        return weight * matched.size to RecommendationReason(code, "${prefixOf(code)} ${quoted(matched)}")
    }

    private fun textPart(
        blob: String,
        keywords: List<String>,
        weight: Double,
        code: RecommendationReasonCode,
    ): Pair<Double, RecommendationReason>? {
        val matched =
            keywords.filter { keyword ->
                val key = keyword.trim().lowercase()
                key.isNotEmpty() && blob.contains(key)
            }
        if (matched.isEmpty()) return null
        return weight * matched.size to RecommendationReason(code, "${prefixOf(code)} ${quoted(matched)}")
    }

    private fun schedulePart(
        tripDayCount: Int,
        availableTripDays: Int,
    ): Pair<Double, RecommendationReason>? {
        if (tripDayCount <= 0 || availableTripDays <= 0) return null
        val available = availableTripDays.coerceAtMost(tripDayCount)
        val ratio = available.toDouble() / tripDayCount
        return WEIGHT_SCHEDULE_FIT * ratio to
            RecommendationReason(
                RecommendationReasonCode.SCHEDULE_FIT,
                "방문 기간 ${tripDayCount}일 중 ${available}일 예약 가능",
            )
    }

    private fun prefixOf(code: RecommendationReasonCode): String =
        when (code) {
            RecommendationReasonCode.BEHAVIOR_TAG -> "찜·리뷰한 상품과 같은 태그"
            RecommendationReasonCode.EXTRA_TAG -> "설문 응답 태그"
            RecommendationReasonCode.PLACE_MATCH -> "방문 장소"
            RecommendationReasonCode.IDOL_MATCH -> "아이돌"
            RecommendationReasonCode.CONCEPT_MATCH -> "선호 컨셉"
            RecommendationReasonCode.SCHEDULE_FIT -> "방문 기간"
        }

    private fun quoted(values: List<String>): String = values.joinToString(", ") { "'$it'" }

    private fun productSearchBlob(product: Product): String {
        val parts = mutableListOf<String>()
        parts.add(product.name)
        parts.add(product.description)
        product.productLocation?.let { loc ->
            parts.add(loc.name)
            parts.add(loc.roadAddressName.orEmpty())
            parts.add(loc.addressName.orEmpty())
        }
        for (po in product.productOptions) {
            parts.add(po.option.name)
            parts.add(po.option.description)
            parts.add(po.option.location)
        }
        return parts.joinToString(" ").lowercase()
    }
}
