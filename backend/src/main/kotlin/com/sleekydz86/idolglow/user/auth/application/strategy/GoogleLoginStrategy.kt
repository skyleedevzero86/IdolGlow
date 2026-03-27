package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.application.userInfo.OAuth2UserInfo
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.stereotype.Service

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
