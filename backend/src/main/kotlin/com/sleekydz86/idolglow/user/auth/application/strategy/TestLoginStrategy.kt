package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.stereotype.Service

@Service
class TestLoginStrategy(
    private val authService: AuthService
) : LoginStrategy {

    override val provider: AuthProvider = AuthProvider.TEST

    override fun login(request: LoginRequest): TokenResponse {
        val email = request.email?.trim()
            ?: throw IllegalArgumentException("테스트 로그인에는 이메일이 필요합니다.")

        val providerId = "test:$email"

        return authService.login(
            AuthProvider.TEST,
            providerId,
            email
        )
    }
}
