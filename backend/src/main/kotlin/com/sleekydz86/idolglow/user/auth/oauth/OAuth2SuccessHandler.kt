package com.sleekydz86.idolglow.user.auth.oauth

import com.sleekydz86.idolglow.user.auth.application.LoginFacade
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
    private val loginFacade: LoginFacade,
    @Value("\${app.oauth2.redirect-uri}")
    private val frontRedirectUri: String,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as CustomOAuth2User
        val provider = principal.provider


        val tokenResponse = loginFacade.login(
            provider = provider,
            request = LoginRequest(attributes = principal.attributes)
        )

        refreshTokenCookieSupporter.addRefreshTokenCookie(response, tokenResponse.refreshToken)
        response.sendRedirect(frontRedirectUri)
    }
}
