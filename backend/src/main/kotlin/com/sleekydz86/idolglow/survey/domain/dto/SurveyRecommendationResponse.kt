package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse

data class SurveyRecommendationResponse(
    val submissionId: Long,
    val llmEnhanced: Boolean,
    val llmPending: Boolean,
    val title: String,
    val subtitle: String,
    val narrative: String,
    val attractions: List<SurveyRecommendedAttractionResponse>,
    val recommendedProducts: List<ProductRankingResponse>,
    val usedSignals: List<SurveyUsedSignalResponse> = emptyList(),
)
