package com.sleekydz86.idolglow.user.auth.ui.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val nickname: String,
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)
