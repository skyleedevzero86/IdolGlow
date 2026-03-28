package com.sleekydz86.idolglow.user.user.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

data class UpdateProfileRequest(
    @field:Schema(description = "닉네임(영문·숫자 2~10). 미전송 시 유지")
    @field:Pattern(regexp = "^[a-zA-Z0-9]{2,10}$", message = "닉네임은 2~10자의 영문 또는 숫자만 가능합니다.")
    val nickname: String? = null,
    @field:Schema(
        description = "프로필 이미지 URL(http/https). 빈 문자열이면 OAuth 기본 이미지로 초기화. 미전송 시 유지"
    )
    val profileImageUrl: String? = null,
)
