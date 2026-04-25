package com.sleekydz86.idolglow.survey.ui.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SubmitSurveyResponseRequest(
    @field:NotEmpty
    val answers: List<@Valid SurveyAnswerRequest>,
)

data class SurveyAnswerRequest(
    @field:NotNull
    val questionId: Long,
    @field:Size(max = 4000)
    val answerText: String? = null,
    val selectedOptions: List<@Size(max = 300) String> = emptyList(),
)
