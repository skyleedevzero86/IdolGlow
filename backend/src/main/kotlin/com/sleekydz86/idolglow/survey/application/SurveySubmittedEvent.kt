package com.sleekydz86.idolglow.survey.application

data class SurveySubmittedEvent(
    val userId: Long,
    val submissionId: Long,
)
