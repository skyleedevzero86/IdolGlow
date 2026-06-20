package com.sleekydz86.idolglow.survey.domain

enum class SurveyFormStatus(
    val label: String,
) {
    PLANNED("예정"),
    SCHEDULED("예정"),
    IN_PROGRESS("진행"),
    COMPLETED("완료"),
}
