package com.sleekydz86.idolglow.survey.infrastructure

data class LlmSurveyRecommendation(
    val title: String? = null,
    val subtitle: String? = null,
    val narrative: String? = null,
    val attractionReasons: Map<String, String> = emptyMap(),
) {
    fun normalize(): LlmSurveyRecommendation =
        copy(
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
            narrative = narrative?.trim()?.takeIf { it.isNotEmpty() },
            attractionReasons =
                attractionReasons
                    .mapValues { (_, value) -> value.trim() }
                    .filterValues { it.isNotEmpty() },
        )
}
