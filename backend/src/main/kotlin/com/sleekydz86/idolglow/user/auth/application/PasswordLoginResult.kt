package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse

data class PasswordLoginResult(
    val token: TokenResponse,
    val requirePasswordChange: Boolean,
)
