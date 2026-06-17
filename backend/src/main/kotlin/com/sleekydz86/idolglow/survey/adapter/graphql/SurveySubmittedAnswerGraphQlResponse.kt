package com.sleekydz86.idolglow.survey.graphql

data class SurveySubmittedAnswerGraphQlResponse(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
