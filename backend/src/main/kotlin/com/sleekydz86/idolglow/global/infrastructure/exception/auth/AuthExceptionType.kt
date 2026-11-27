package com.sleekydz86.idolglow.global.infrastructure.exception.auth

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class AuthExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    UNAUTHENTICATED(
        "UNAUTHENTICATED",
        "로그인이 필요합니다.",
        401,
    ),

    INVALID_AUTH_PRINCIPAL(
        "INVALID_AUTH_PRINCIPAL",
        "인증 정보가 올바르지 않습니다.",
        401,
    ),

    INVALID_REFRESH_TOKEN(
        "INVALID_REFRESH_TOKEN",
        "유효하지 않은 리프레시 토큰입니다.",
        401,
    ),

    INVALID_TOKEN_TYPE(
        "INVALID_TOKEN_TYPE",
        "토큰 타입이 올바르지 않습니다.",
        401,
    ),

    INVALID_REFRESH_CSRF(
        "INVALID_REFRESH_CSRF",
        "새로고침 요청 검증에 실패했습니다.",
        403,
    ),

    USER_NOT_FOUND(
        "USER_NOT_FOUND",
        "사용자를 찾을 수 없습니다.",
        404,
    ),

    KAKAO_EMAIL_REQUIRED(
        "KAKAO_EMAIL_REQUIRED",
        "카카오 이메일 제공에 동의하고 인증된 이메일이 있어야 가입할 수 있습니다.",
        BAD_REQUEST_STATUS,
    ),

    NAVER_EMAIL_REQUIRED(
        "NAVER_EMAIL_REQUIRED",
        "네이버 이메일 제공에 동의해야 가입할 수 있습니다.",
        BAD_REQUEST_STATUS,
    ),
}

private const val BAD_REQUEST_STATUS = 400
