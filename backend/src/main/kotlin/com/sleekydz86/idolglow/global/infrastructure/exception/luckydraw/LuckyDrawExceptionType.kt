package com.sleekydz86.idolglow.global.infrastructure.exception.luckydraw

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class LuckyDrawExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    PERIOD_OVERLAP("LUCKY_DRAW_PERIOD_OVERLAP", "응모 기간이 겹치는 추첨이 이미 있습니다.", CONFLICT),
    ALREADY_STARTED("LUCKY_DRAW_ALREADY_STARTED", "응모가 시작된 추첨은 수정하거나 삭제할 수 없습니다.", CONFLICT),
    NOT_OPEN("LUCKY_DRAW_NOT_OPEN", "지금은 응모 기간이 아닙니다.", CONFLICT),
    ALREADY_ENTERED("LUCKY_DRAW_ALREADY_ENTERED", "오늘은 이미 응모했습니다. 내일 다시 응모해 주세요.", CONFLICT),
    NOT_FOUND("LUCKY_DRAW_NOT_FOUND", "추첨을 찾을 수 없습니다.", NOT_FOUND_STATUS),
}

private const val CONFLICT = 409
private const val NOT_FOUND_STATUS = 404
