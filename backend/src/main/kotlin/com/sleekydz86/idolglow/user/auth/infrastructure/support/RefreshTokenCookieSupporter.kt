package com.sleekydz86.idolglow.user.auth.infrastructure.support

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RefreshTokenCookieSupporter(
    @Value("\${app.auth.refresh-cookie.secure:false}")
    private val secure: Boolean,
    @Value("\${app.auth.refresh-cookie.same-site:Lax}")
    private val sameSite: String,
    @Value("\${app.auth.refresh-cookie.domain:}")
    private val domain: String,
) {

    fun addAuthenticationCookies(
        response: HttpServletResponse,
        tokenResponse: TokenResponse,
    ) {
        addAccessTokenCookie(response, tokenResponse.accessToken, tokenResponse.accessTokenExpiresIn)
        addRefreshTokenCookie(response, tokenResponse.refreshToken)
    }

    fun addAccessTokenCookie(
        response: HttpServletResponse,
        accessToken: String,
        expiresAtMillis: Long,
    ) {
        val maxAge = ((expiresAtMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(ACCESS_TOKEN_COOKIE, accessToken, true, maxAge).toString()
        )
    }

    fun addRefreshTokenCookie(
        response: HttpServletResponse,
        refreshToken: String
    ) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, true, 60L * 60 * 24 * 14).toString()
        )
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(REFRESH_CSRF_COOKIE, UUID.randomUUID().toString(), false, 60L * 60 * 24 * 14).toString()
        )
    }

    fun expireAuthenticationCookies(response: HttpServletResponse) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(ACCESS_TOKEN_COOKIE, "", true, 0).toString()
        )
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(REFRESH_TOKEN_COOKIE, "", true, 0).toString()
        )
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(REFRESH_CSRF_COOKIE, "", false, 0).toString()
        )
    }

    fun validateCsrf(headerValue: String?, cookieValue: String?) {
        if (headerValue.isNullOrBlank() || cookieValue.isNullOrBlank() || headerValue != cookieValue) {
            throw CustomException(AuthExceptionType.INVALID_REFRESH_CSRF)
        }
    }

    private fun buildCookie(
        name: String,
        value: String,
        httpOnly: Boolean,
        maxAge: Long
    ): ResponseCookie {
        val builder = ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(secure)
            .path("/")
            .sameSite(sameSite)
            .maxAge(maxAge)

        if (domain.isNotBlank()) {
            builder.domain(domain)
        }

        return builder.build()
    }

    companion object {
        const val ACCESS_TOKEN_COOKIE = "accessToken"
        const val REFRESH_TOKEN_COOKIE = "refreshToken"
        const val REFRESH_CSRF_COOKIE = "refreshCsrfToken"
        const val REFRESH_CSRF_HEADER = "X-Refresh-CSRF"
    }
}
