package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.application.userInfo.KakaoUserInfo
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.stereotype.Service

@Service
class KakaoLoginStrategy(
    private val authService: AuthService,
) : LoginStrategy {
    override val provider: AuthProvider = AuthProvider.KAKAO

    override fun login(request: LoginRequest): TokenResponse {
        val attributes =
            request.attributes
                ?: throw IllegalArgumentException("카카오 로그인 속성 정보가 필요합니다.")
        val userInfo = KakaoUserInfo(attributes)
        val providerId = userInfo.id
        val email = userInfo.usableEmail
        if (email == null && !authService.isLinked(AuthProvider.KAKAO, providerId)) {
            throw CustomException(AuthExceptionType.KAKAO_EMAIL_REQUIRED)
        }
        return authService.login(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
            email = email.orEmpty(),
            name = userInfo.name,
            picture = userInfo.picture,
        )
    }
}
