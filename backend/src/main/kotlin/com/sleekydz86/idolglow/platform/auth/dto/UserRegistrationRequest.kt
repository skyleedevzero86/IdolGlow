package com.sleekydz86.idolglow.platform.auth.dto

import com.sleekydz86.idolglow.platform.user.domain.PlatformNotificationType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserRegistrationRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val password: String,
    @field:NotBlank(message = "사용자명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "사용자명은 2자 이상 50자 이하여야 합니다")
    val username: String,
    val nickname: String? = null,
    val notificationPreferences: List<PlatformNotificationType>? = null,
)
