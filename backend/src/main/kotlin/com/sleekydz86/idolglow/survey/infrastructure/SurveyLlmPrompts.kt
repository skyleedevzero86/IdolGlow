package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import tools.jackson.databind.ObjectMapper

object SurveyLlmPrompts {

    val system: String =
        """
        한국 여행 추천 카피라이터 역할로 동작하세요.
        반드시 JSON 객체만 출력하세요.
        형식:
        {
          "title":"...",
          "subtitle":"...",
          "narrative":"...",
          "attractionReasons":{"<attractionCode>":"..."}
        }
        """.trimIndent()

    fun userJson(
        objectMapper: ObjectMapper,
        titleFallback: String,
        subtitleFallback: String,
        narrativeFallback: String,
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
    ): String =
        objectMapper.writeValueAsString(
            mapOf(
                "fallback" to mapOf(
                    "title" to titleFallback,
                    "subtitle" to subtitleFallback,
                    "narrative" to narrativeFallback,
                ),
                "answerHighlights" to answerHighlights,
                "attractions" to attractions.map {
                    mapOf(
                        "attractionCode" to it.attractionCode,
                        "name" to it.name,
                        "areaName" to it.areaName,
                        "signguName" to it.signguName,
                        "categoryLarge" to it.categoryLarge,
                        "categoryMiddle" to it.categoryMiddle,
                        "rank" to it.rank,
                    )
                },
            ),
        )
}
