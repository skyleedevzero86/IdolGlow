package com.sleekydz86.idolglow.user.auth.adapter.web

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증", description = "OAuth2 콜백 처리 API")
@RestController
@RequestMapping("/auth")
class OAuthCallbackController(
    private val authService: AuthService,
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
    @Value("\${app.oauth2.redirect-uri}")
    private val frontRedirectUri: String,
) {
    @SecurityRequirements
    @Operation(summary = "OAuth2 콜백", description = "refreshToken 쿠키로 accessToken을 재발급한 뒤 프론트로 리다이렉트합니다.")
    @ApiResponse(responseCode = "302", description = "프론트 콜백 URL로 리다이렉트")
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
