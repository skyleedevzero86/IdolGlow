package com.sleekydz86.idolglow.user.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원가입 중복 확인 결과")
data class SignupCheckResponse(
    @field:Schema(description = "사용 가능 여부")
    val available: Boolean,
    @field:Schema(description = "불가 시 사유: BLANK, INVALID_FORMAT, TAKEN", example = "TAKEN")
    val code: String? = null,
)
