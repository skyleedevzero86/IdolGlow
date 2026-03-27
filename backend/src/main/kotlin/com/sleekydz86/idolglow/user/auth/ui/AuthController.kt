package com.sleekydz86.idolglow.user.auth.ui

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.domain.dto.AccessTokenResponse
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
) : AuthApi {

    @GetMapping("/login/{provider}")
    override fun login(response: HttpServletResponse, @PathVariable("provider") provider: String) {
        if (!AuthProvider.isAllowedRegistrationId(provider)) {
            throw IllegalArgumentException("지원하지 않는 로그인 제공자입니다: $provider")
        }
        response.sendRedirect("/oauth2/authorization/$provider")
    }

    @PostMapping("/reissue")
    override fun reissue(
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_CSRF_COOKIE, required = false) refreshCsrfToken: String?,
        @RequestHeader(value = RefreshTokenCookieSupporter.REFRESH_CSRF_HEADER, required = false) refreshCsrfHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<AccessTokenResponse> {
        refreshTokenCookieSupporter.validateCsrf(refreshCsrfHeader, refreshCsrfToken)

        val tokenResponse = authService.reissue(refreshToken.orEmpty())
        refreshTokenCookieSupporter.addRefreshTokenCookie(response, tokenResponse.refreshToken)

        return ResponseEntity.ok(AccessTokenResponse.from(tokenResponse))
    }

    @PostMapping("/logout")
    override fun logout(
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_CSRF_COOKIE, required = false) refreshCsrfToken: String?,
        @RequestHeader(value = RefreshTokenCookieSupporter.REFRESH_CSRF_HEADER, required = false) refreshCsrfHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        refreshTokenCookieSupporter.validateCsrf(refreshCsrfHeader, refreshCsrfToken)
        refreshTokenCookieSupporter.expireRefreshTokenCookie(response)
        return ResponseEntity.ok().build()
    }
}
