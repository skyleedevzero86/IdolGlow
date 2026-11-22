package com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class EventExternalIdExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    EVENT_NOT_FOUND("EVENT_EXTERNAL_ID_EVENT_NOT_FOUND", "공연을 찾을 수 없습니다.", NOT_FOUND_STATUS),
    ALREADY_LINKED("EVENT_EXTERNAL_ID_ALREADY_LINKED", "이 외부 ID는 이미 다른 공연에 연결되어 있습니다.", CONFLICT_STATUS),
    NOT_LINKED("EVENT_EXTERNAL_ID_NOT_LINKED", "이 공연에 연결된 외부 ID가 아닙니다.", NOT_FOUND_STATUS),
}

private const val NOT_FOUND_STATUS = 404
private const val CONFLICT_STATUS = 409
