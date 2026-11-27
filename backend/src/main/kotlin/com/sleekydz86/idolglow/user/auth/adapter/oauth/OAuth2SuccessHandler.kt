package com.sleekydz86.idolglow.user.auth.adapter.oauth

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.application.LoginFacade
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
    private val loginFacade: LoginFacade,
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String,
) : AuthenticationSuccessHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauthToken =
            authentication as? OAuth2AuthenticationToken
                ?: throw IllegalStateException(
                    "OAuth2 인증 토큰 형식이 올바르지 않습니다. (실제 타입: ${authentication.javaClass.name})",
                )
        val principal =
            oauthToken.principal as? OAuth2User
                ?: throw IllegalStateException(
                    "OAuth2 사용자 정보(principal) 형식이 올바르지 않습니다. (실제 타입: ${(oauthToken.principal as Any?)?.javaClass?.name})",
                )

        val provider = AuthProvider.fromRegistrationId(oauthToken.authorizedClientRegistrationId)
        val attributes = oauthAttributes(principal)

        val tokenResponse =
            try {
                loginFacade.login(
                    provider = provider,
                    request = LoginRequest(attributes = attributes),
                )
            } catch (exception: CustomException) {
                if (exception.getExceptionType() != AuthExceptionType.KAKAO_EMAIL_REQUIRED) throw exception
                log.warn("카카오 이메일 없음으로 로그인 거절 providerId={}", oauthToken.name)
                response.sendRedirect(errorRedirect(KAKAO_EMAIL_ERROR))
                return
            }

        refreshTokenCookieSupporter.addRefreshTokenCookie(response, tokenResponse.refreshToken)
        response.sendRedirect(AUTH_CALLBACK_PATH)
    }

    private fun oauthAttributes(principal: OAuth2User): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        if (principal is OidcUser) {
            principal.idToken.claims.forEach { (k, v) -> out[k] = v as Any }
            principal.userInfo?.claims?.forEach { (k, v) -> out.putIfAbsent(k, v as Any) }
        }
        principal.attributes.forEach { (k, v) -> out.putIfAbsent(k, v as Any) }
        return out
    }

    private fun errorRedirect(code: String): String {
        val base = redirectUri.trim()
        val separator = if (base.contains("?")) "&" else "?"
        return "$base${separator}oauth_error=$code"
    }

    companion object {
        private const val AUTH_CALLBACK_PATH = "/auth/callback"
        private const val KAKAO_EMAIL_ERROR = "kakao_email"
    }
}
