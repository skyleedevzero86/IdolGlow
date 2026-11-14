package com.sleekydz86.idolglow.survey.domain.dto

data class SurveyUsedSignalResponse(
    val type: SurveyUsedSignalType,
    val source: SurveyUsedSignalSource,
    val values: List<String>,
)

enum class SurveyUsedSignalType {
    CONCEPT,
    IDOL,
    PLACE,
    TAG,
    BEHAVIOR_TAG,
    VISIT_PERIOD,
}

enum class SurveyUsedSignalSource {
    FORM,
    USER_SURVEY,
    ACTIVITY,
}
