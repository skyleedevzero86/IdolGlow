package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import org.springframework.stereotype.Service
import java.security.AuthProvider

@Service
class GoogleLoginStrategy(
    private val authService: AuthService
) : LoginStrategy {

    override val provider: AuthProvider = AuthProvider.GOOGLE

    override fun login(request: LoginRequest): TokenResponse {
        val attributes = request.attributes
            ?: throw IllegalArgumentException("attributes required")
        val userInfo = OAuth2UserInfo.of(AuthProvider.GOOGLE, attributes)

        return authService.login(
            provider = AuthProvider.GOOGLE,
            providerId = userInfo.id,
            email = userInfo.email
        )
    }
}
