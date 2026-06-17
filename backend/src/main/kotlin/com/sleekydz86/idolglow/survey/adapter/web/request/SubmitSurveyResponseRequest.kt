package com.sleekydz86.idolglow.survey.ui.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class SubmitSurveyResponseRequest(
    @field:NotEmpty
    val answers: List<@Valid SurveyAnswerRequest>,
)
