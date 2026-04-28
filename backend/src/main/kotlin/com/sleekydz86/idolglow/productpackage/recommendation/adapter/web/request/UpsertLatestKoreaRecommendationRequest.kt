package com.sleekydz86.idolglow.productpackage.recommendation.adapter.web.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class UpsertLatestKoreaRecommendationRequest(
    @field:NotEmpty
    @field:Size(max = 50)
    val productIds: List<@Positive Long>,
)
