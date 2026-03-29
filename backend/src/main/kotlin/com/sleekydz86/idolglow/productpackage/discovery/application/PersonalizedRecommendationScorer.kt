package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import kotlin.math.ln

object PersonalizedRecommendationScorer {

    private const val WEIGHT_CONCEPT_TAG = 4.0
    private const val WEIGHT_BEHAVIOR_TAG = 2.5
    private const val WEIGHT_PLACE = 2.0
    private const val WEIGHT_IDOL_TEXT = 1.2
    private const val WEIGHT_SCHEDULE_FIT = 5.0
    private const val WEIGHT_WISH_LOG = 0.35
    private const val WEIGHT_RATING = 0.45
    private const val WEIGHT_REVIEW_LOG = 0.15

    fun score(
        survey: UserSurvey?,
        behaviorTags: Set<String>,
        conceptTag: String?,
        placeKeywords: List<String>,
        productTags: Set<String>,
        product: Product,
        availableTripDays: Int,
        tripDayCount: Int,
        wishCount: Long,
        averageRating: Double,
        reviewCount: Long,
    ): Double {
        var score = 0.0

        if (conceptTag != null && conceptTag in productTags) {
            score += WEIGHT_CONCEPT_TAG
        }

        val behaviorMatches = productTags.count { it in behaviorTags }
        score += WEIGHT_BEHAVIOR_TAG * behaviorMatches

        val blob = productSearchBlob(product)
        if (placeKeywords.isNotEmpty()) {
            val hits = placeKeywords.count { kw ->
                val k = kw.trim().lowercase()
                k.isNotEmpty() && blob.contains(k)
            }
            score += WEIGHT_PLACE * hits
        }

        if (survey != null) {
            for (token in idolTokens(survey.idolName)) {
                if (blob.contains(token)) {
                    score += WEIGHT_IDOL_TEXT
                }
            }
        }

        if (tripDayCount > 0) {
            val ratio = (availableTripDays.toDouble() / tripDayCount).coerceIn(0.0, 1.0)
            score += WEIGHT_SCHEDULE_FIT * ratio
        }

        score += WEIGHT_WISH_LOG * ln((wishCount + 1).toDouble())
        score += WEIGHT_RATING * averageRating.coerceIn(0.0, 5.0)
        score += WEIGHT_REVIEW_LOG * ln((reviewCount + 1).toDouble())

        return score
    }

    fun matchedPreferenceTags(
        productTags: List<String>,
        behaviorTags: Set<String>,
        conceptTag: String?,
    ): List<String> {
        val signals = buildSet {
            addAll(behaviorTags)
            if (conceptTag != null) add(conceptTag)
        }
        return productTags.filter { it in signals }.distinct()
    }

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

    private fun idolTokens(idolName: String): List<String> =
        idolName.split(Regex("[\\s,]+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }
            .distinct()
            .take(8)
}
