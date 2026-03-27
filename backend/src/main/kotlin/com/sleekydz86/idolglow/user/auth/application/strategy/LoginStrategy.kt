package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider

interface LoginStrategy {
    val provider: AuthProvider
    fun login(request: LoginRequest): TokenResponse
}
