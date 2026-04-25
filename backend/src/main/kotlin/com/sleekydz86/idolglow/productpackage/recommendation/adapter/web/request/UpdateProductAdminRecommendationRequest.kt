package com.sleekydz86.idolglow.productpackage.recommendation.adapter.web.request

import jakarta.validation.constraints.NotNull

data class UpdateProductAdminRecommendationRequest(
    @field:NotNull
    val recommended: Boolean,
)
