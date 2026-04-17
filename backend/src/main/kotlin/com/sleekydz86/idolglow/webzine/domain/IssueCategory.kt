package com.sleekydz86.idolglow.webzine.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class IssueCategory(
    @get:JsonValue
    val code: String,
    val label: String,
) {
    EXHIBITION("exhibition", "전시"),
    PERFORMANCE("performance", "공연"),
    FORUM("forum", "교육·포럼"),
    EVENT("event", "행사·교류"),
    ARTICLE("article", "아티클"),
    VIDEO("video", "비디오"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun from(value: String): IssueCategory =
            entries.firstOrNull { it.code.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("지원하지 않는 웹진 카테고리입니다. category=$value")
    }
}
