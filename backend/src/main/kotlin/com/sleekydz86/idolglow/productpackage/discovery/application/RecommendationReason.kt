package com.sleekydz86.idolglow.productpackage.discovery.application

enum class RecommendationReasonCode {
    CONCEPT_MATCH,
    BEHAVIOR_TAG,
    PLACE_MATCH,
    IDOL_MATCH,
    SCHEDULE_FIT,
    EXTRA_TAG,
}

enum class RecommendationFallback {
    POPULAR,
}

data class RecommendationReason(
    val code: RecommendationReasonCode,
    val label: String,
)

data class RecommendationScore(
    val score: Double,
    val reasons: List<RecommendationReason>,
)

data class RecommendationProductMetrics(
    val availableTripDays: Int,
    val wishCount: Long,
    val averageRating: Double,
    val reviewCount: Long,
)
