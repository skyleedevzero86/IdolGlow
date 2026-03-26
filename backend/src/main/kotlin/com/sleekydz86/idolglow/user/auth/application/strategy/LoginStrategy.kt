package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import java.security.AuthProvider

interface LoginStrategy {
    val provider: AuthProvider
    fun login(request: LoginRequest): TokenResponse
}
