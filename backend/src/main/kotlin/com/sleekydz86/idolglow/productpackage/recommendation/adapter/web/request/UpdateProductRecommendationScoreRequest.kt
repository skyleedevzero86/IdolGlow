package com.sleekydz86.idolglow.productpackage.recommendation.adapter.web.request

import jakarta.validation.constraints.Min

data class UpdateProductRecommendationScoreRequest(
    @field:Min(0)
    val score: Int,
)
