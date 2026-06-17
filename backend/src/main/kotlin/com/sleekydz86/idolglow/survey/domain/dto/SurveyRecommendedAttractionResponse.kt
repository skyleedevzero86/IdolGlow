package com.sleekydz86.idolglow.survey.domain.dto

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
