package com.sleekydz86.idolglow.global.exceptions

enum class UserExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int
) : CustomExceptionType {

    INVALID_NICKNAME(
        "INVALID_NICKNAME",
        "닉네임은 2~10자의 영문 또는 숫자만 가능합니다.",
        400
    ),
    INVALID_PROFILE_IMAGE_URL(
        "INVALID_PROFILE_IMAGE_URL",
        "프로필 이미지는 http 또는 https URL 이어야 합니다.",
        400
    )
}
