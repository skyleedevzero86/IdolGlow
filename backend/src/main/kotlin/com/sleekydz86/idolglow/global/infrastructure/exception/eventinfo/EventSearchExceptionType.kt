package com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class EventSearchExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    DISABLED("EVENT_SEARCH_DISABLED", "공연 검색이 꺼져 있습니다.", UNAVAILABLE_STATUS),
    FAILED("EVENT_SEARCH_FAILED", "공연 검색 엔진에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.", UNAVAILABLE_STATUS),
}

private const val UNAVAILABLE_STATUS = 503
