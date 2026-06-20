package com.sleekydz86.idolglow.survey.domain.dto

data class SurveySubmittedAnswerResponse(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
