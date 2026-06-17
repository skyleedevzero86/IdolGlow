package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse

data class SurveyRecommendationResponse(
    val submissionId: Long,
    val llmEnhanced: Boolean,
    val title: String,
    val subtitle: String,
    val narrative: String,
    val attractions: List<SurveyRecommendedAttractionResponse>,
    val recommendedProducts: List<ProductPagingQueryResponse>,
)
