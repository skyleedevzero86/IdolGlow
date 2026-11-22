package com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class EventMetaDeadLetterExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    NOT_FOUND("EVENT_META_DEAD_LETTER_NOT_FOUND", "실패한 공연 메타 메시지를 찾을 수 없습니다.", NOT_FOUND_STATUS),
    ALREADY_REPROCESSED("EVENT_META_DEAD_LETTER_ALREADY_REPROCESSED", "이미 다시 처리한 메시지입니다.", CONFLICT_STATUS),
    UNKNOWN_CONSUMER("EVENT_META_DEAD_LETTER_UNKNOWN_CONSUMER", "다시 처리할 소비자를 알 수 없는 메시지입니다.", CONFLICT_STATUS),
    REPROCESS_FAILED("EVENT_META_DEAD_LETTER_REPROCESS_FAILED", "다시 처리에 실패했습니다. 원인을 고친 뒤 다시 시도해 주세요.", CONFLICT_STATUS),
}

private const val NOT_FOUND_STATUS = 404
private const val CONFLICT_STATUS = 409
