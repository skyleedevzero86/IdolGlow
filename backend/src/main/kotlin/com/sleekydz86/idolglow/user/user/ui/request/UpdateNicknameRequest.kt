package com.sleekydz86.idolglow.user.user.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class UpdateNicknameRequest(
    @field:Schema(description = "닉네임", example = "IdolGlow")
    @field:NotBlank
    val nickname: String
)
