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

data class SurveyRecommendedAttractionResponse(
    val attractionCode: String,
    val name: String,
    val areaName: String?,
    val signguName: String?,
    val categoryLarge: String?,
    val categoryMiddle: String?,
    val rank: Int,
    val reason: String,
)
