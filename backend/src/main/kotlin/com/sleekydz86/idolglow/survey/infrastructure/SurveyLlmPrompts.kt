package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import tools.jackson.databind.ObjectMapper

object SurveyLlmPrompts {
    val system: String =
        """
        한국 여행 추천 카피라이터 역할로 동작하세요.
        입력의 fillThis JSON에서 빈 문자열만 채워 그대로 출력하세요. 키를 추가하거나 빼지 마세요.
        attractions 목록과 answerHighlights에 있는 내용만 쓰세요. 목록에 없는 장소·가게·행사·가격·숫자는 쓰지 마세요.
        관광지 이름은 그대로 쓰고, 자연스러운 한국어 존댓말로 쓰세요. 한자·일본어·다른 외국어 문장은 쓰지 마세요.
        title 30자 이내, subtitle 40자 이내, narrative 2문장, attractionReasons 값은 각 1문장.
        """.trimIndent()

    fun userJson(
        objectMapper: ObjectMapper,
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
    ): String =
        objectMapper.writeValueAsString(
            mapOf(
                "answerHighlights" to answerHighlights,
                "attractions" to
                    attractions.map {
                        mapOf(
                            "attractionCode" to it.attractionCode,
                            "name" to it.name,
                            "signguName" to it.signguName,
                            "categoryMiddle" to it.categoryMiddle,
                        )
                    },
                "fillThis" to
                    mapOf(
                        "title" to "",
                        "subtitle" to "",
                        "narrative" to "",
                        "attractionReasons" to attractions.associate { it.attractionCode to "" },
                    ),
            ),
        )
}
