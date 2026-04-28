package com.sleekydz86.idolglow.user.auth.ui

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class OAuthCallbackController(
    private val authService: AuthService,
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
    @Value("\${app.oauth2.redirect-uri}")
    private val frontRedirectUri: String,
) {

    @GetMapping("/callback")
    fun callback(
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ) {
        val redirectTarget = frontRedirectUri.trim()
        if (refreshToken.isNullOrBlank()) {
            response.sendRedirect(withOauthError(redirectTarget))
            return
        }

        runCatching {
            authService.reissue(refreshToken)
        }.onSuccess { tokenResponse ->
            refreshTokenCookieSupporter.addAuthenticationCookies(response, tokenResponse)
            response.sendRedirect(redirectTarget)
        }.onFailure {
            refreshTokenCookieSupporter.expireAuthenticationCookies(response)
            response.sendRedirect(withOauthError(redirectTarget))
        }
    }

    private fun withOauthError(base: String): String {
        val separator = if (base.contains("?")) "&" else "?"
        return "$base${separator}oauth_error=1"
    }
}
