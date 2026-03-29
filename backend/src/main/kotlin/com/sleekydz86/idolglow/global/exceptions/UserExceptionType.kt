package com.sleekydz86.idolglow.global.exceptions

enum class UserExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int
) : CustomExceptionType {

    INVALID_NICKNAME(
        "INVALID_NICKNAME",
        "닉네임은 2~10자의 한글·영문 또는 숫자만 가능합니다.",
        400
    ),

    INVALID_EMAIL(
        "INVALID_EMAIL",
        "올바른 이메일 주소를 입력해 주세요.",
        400
    ),
    INVALID_PROFILE_IMAGE_URL(
        "INVALID_PROFILE_IMAGE_URL",
        "프로필 이미지는 http 또는 https URL 이어야 합니다.",
        400
    ),

    EMAIL_ALREADY_REGISTERED(
        "EMAIL_ALREADY_REGISTERED",
        "이미 가입된 이메일입니다.",
        409
    ),

    NICKNAME_ALREADY_REGISTERED(
        "NICKNAME_ALREADY_REGISTERED",
        "등록된 별명입니다.",
        409
    ),

    SIGNUP_PASSWORD_POLICY(
        "SIGNUP_PASSWORD_POLICY",
        "비밀번호는 8자 이상이며 영문과 숫자를 각각 1자 이상 포함해야 합니다.",
        400
    ),

    SIGNUP_PASSWORD_REQUIRED(
        "SIGNUP_PASSWORD_REQUIRED",
        "비밀번호를 입력해 주세요.",
        400
    ),

    PROFILE_IMAGE_INVALID_TYPE(
        "PROFILE_IMAGE_INVALID_TYPE",
        "프로필 이미지는 JPEG, PNG, WebP 형식만 업로드할 수 있습니다.",
        400
    ),

    PROFILE_IMAGE_TOO_LARGE(
        "PROFILE_IMAGE_TOO_LARGE",
        "프로필 이미지는 5MB 이하여야 합니다.",
        400
    )
}
