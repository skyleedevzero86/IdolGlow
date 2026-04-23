package com.sleekydz86.idolglow.user.user.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:Schema(description = "현재 비밀번호")
    @field:NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    val currentPassword: String,
    @field:Schema(description = "새 비밀번호 (8~72자, 영문·숫자 각 1자 이상)")
    @field:NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @field:Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
    val newPassword: String,
)
