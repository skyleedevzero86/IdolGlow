package com.sleekydz86.idolglow.global.exceptions.auth


enum class AuthExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int
) : CustomExceptionType {

    UNAUTHENTICATED(
        "UNAUTHENTICATED",
        "로그인이 필요합니다.",
        401
    ),

    INVALID_AUTH_PRINCIPAL(
        "INVALID_AUTH_PRINCIPAL",
        "인증 정보가 올바르지 않습니다.",
        401
    ),

    INVALID_REFRESH_TOKEN(
        "INVALID_REFRESH_TOKEN",
        "유효하지 않은 리프레시 토큰입니다.",
        401
    ),

    INVALID_TOKEN_TYPE(
        "INVALID_TOKEN_TYPE",
        "토큰 타입이 올바르지 않습니다.",
        401
    ),

    INVALID_REFRESH_CSRF(
        "INVALID_REFRESH_CSRF",
        "재발급 요청 검증에 실패했습니다.",
        403
    ),

    USER_NOT_FOUND(
        "USER_NOT_FOUND",
        "사용자를 찾을 수 없습니다.",
        404
    ),
}
