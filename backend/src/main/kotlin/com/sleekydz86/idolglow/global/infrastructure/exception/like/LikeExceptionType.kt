package com.sleekydz86.idolglow.global.infrastructure.exception.like

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class LikeExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    INVALID_TARGET_TYPE(
        "LIKE_INVALID_TARGET_TYPE",
        "좋아요를 누를 수 없는 콘텐츠 유형입니다.",
        BAD_REQUEST,
    ),
    INVALID_TARGET_KEY(
        "LIKE_INVALID_TARGET_KEY",
        "콘텐츠 키가 올바르지 않습니다.",
        BAD_REQUEST,
    ),
    TARGET_NOT_FOUND(
        "LIKE_TARGET_NOT_FOUND",
        "좋아요를 누를 콘텐츠를 찾을 수 없습니다.",
        NOT_FOUND,
    ),
}

private const val BAD_REQUEST = 400
private const val NOT_FOUND = 404
