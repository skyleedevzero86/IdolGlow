package com.sleekydz86.idolglow.platform.auth.dto

import jakarta.validation.constraints.NotBlank

data class PasswordResetRequest(
    @field:NotBlank(message = "복구 토큰은 필수입니다")
    val recoveryToken: String,
    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    val newPassword: String,
    @field:NotBlank(message = "비밀번호 확인은 필수입니다")
    val confirmPassword: String,
)
