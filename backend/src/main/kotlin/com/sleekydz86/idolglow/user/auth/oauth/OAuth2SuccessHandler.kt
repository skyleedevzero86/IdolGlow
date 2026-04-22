package com.sleekydz86.idolglow.user.auth.oauth

import com.sleekydz86.idolglow.user.auth.application.LoginFacade
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauthToken = authentication as? OAuth2AuthenticationToken
            ?: throw IllegalStateException(
                "OAuth2 인증 토큰 형식이 올바르지 않습니다. (실제 타입: ${authentication.javaClass.name})"
            )
        val principal = oauthToken.principal as? OAuth2User
            ?: throw IllegalStateException(
                "OAuth2 사용자 정보(principal) 형식이 올바르지 않습니다. (실제 타입: ${(oauthToken.principal as Any?)?.javaClass?.name})"
            )

        val provider = AuthProvider.fromRegistrationId(oauthToken.authorizedClientRegistrationId)
        val attributes = oauthAttributes(principal)

        val tokenResponse = loginFacade.login(
            provider = provider,
            request = LoginRequest(attributes = attributes)
        )

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

    companion object {
        private const val AUTH_CALLBACK_PATH = "/auth/callback"
    }
}
