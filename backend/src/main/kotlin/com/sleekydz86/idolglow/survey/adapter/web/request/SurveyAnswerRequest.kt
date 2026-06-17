package com.sleekydz86.idolglow.survey.ui.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SurveyAnswerRequest(
    @field:NotNull
    val questionId: Long,
    @field:Size(max = 4000)
    val answerText: String? = null,
    val selectedOptions: List<@Size(max = 300) String> = emptyList(),
)
