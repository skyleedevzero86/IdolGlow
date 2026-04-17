package com.sleekydz86.idolglow.platform.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AccountRecoveryRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
    @field:NotBlank(message = "사용자명은 필수입니다")
    val username: String,
)
