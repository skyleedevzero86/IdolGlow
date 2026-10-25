package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt

object GroundedLlmPrompts {
    const val EXTRACT_SYSTEM: String =
        "여행 조건만 JSON으로 답한다. 키는 region, dateFrom, dateTo, maxBudget, category, people 이다. 없는 값은 null. 날짜는 yyyy-MM-dd, 지역은 서울 구 이름, 예산은 숫자만 쓴다."

    const val ANSWER_SYSTEM: String =
        "상품 설명만 답한다. 근거는 [C1]처럼 문단 번호만 인용한다. 가격, 금액, 원, 예약 가능 시간, 날짜는 쓰지 않는다."

    fun extractUser(query: String): String = query

    fun answerUser(query: String, citations: List<GroundedCitationPrompt>): String =
        buildString {
            appendLine(query)
            citations.forEach { citation ->
                append('[').append(citation.label).append("] ").appendLine(citation.content)
            }
        }
}
