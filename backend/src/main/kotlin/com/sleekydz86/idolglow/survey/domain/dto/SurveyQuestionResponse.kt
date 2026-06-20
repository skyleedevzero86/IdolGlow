package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType

data class SurveyQuestionResponse(
    val id: Long,
    val order: Int,
    val title: String,
    val description: String?,
    val type: SurveyQuestionType,
    val required: Boolean,
    val options: List<String>,
)
