package com.sleekydz86.idolglow.user.auth.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

data class TestSignupResponse(
    @field:Schema(description = "회원 ID", example = "1")
    val userId: Long,

    @field:Schema(description = "이메일", example = "test@test.com")
    val email: String
)
