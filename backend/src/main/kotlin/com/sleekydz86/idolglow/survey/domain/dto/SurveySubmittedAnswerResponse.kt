package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import java.time.LocalDateTime

data class SurveySubmittedAnswerResponse(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
